package com.aferent.auth_service.service;

import com.aferent.auth_service.exception.ApiException;
import com.aferent.auth_service.repository.UserRepository;
import com.aferent.auth_service.dto.AuthResponse;
import com.aferent.auth_service.dto.LoginRequest;
import com.aferent.auth_service.dto.RegisterRequest;
import com.aferent.auth_service.model.User;
import com.aferent.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public AuthResponse register(RegisterRequest request) {
                log.info("Processing register request for email={}, role={}", request.getEmail(), request.getRole());

        // Check email not already taken
        if (userRepository.existsByEmail(request.getEmail())) {
                        throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

                User.Role requestedRole;
                try {
                        requestedRole = User.Role.valueOf(request.getRole().toUpperCase());
                } catch (IllegalArgumentException ex) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role");
                }

                if (requestedRole == User.Role.ADMIN) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "Admin registration is not allowed");
                }

                boolean shouldActivate = requestedRole == User.Role.PATIENT;

        // Build the User document
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // bcrypt hash
                .role(requestedRole)
                .active(shouldActivate)
                .activatedAt(shouldActivate ? Instant.now() : null)
                .build();

        // Save to MongoDB
        User saved = userRepository.save(user);

        // Publish user.registered event to Kafka
        // patient-service and doctor-service services listen to this event
        Map<String, Object> event = new HashMap<>();
        event.put("authId", saved.getId());
        event.put("email", saved.getEmail());
        event.put("role", saved.getRole().name());
        kafkaTemplate.send("user.registered", saved.getId(), event);
        log.info("Published user.registered event for authId={}, role={}", saved.getId(), saved.getRole().name());

        //  Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
            saved.getId(), saved.getEmail(), saved.getRole().name(), saved.isActive(), saved.getRefreshTokenVersion()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .role(saved.getRole().name())
                .authId(saved.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        // Check password against hash — NEVER compare plain text
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            // same message intentionally — don't reveal which field is wrong
        }

        if (!user.isActive()) {
            if (user.getRole() == User.Role.DOCTOR) {
                                throw new ApiException(HttpStatus.FORBIDDEN, "Account pending admin verification");
            }
                        throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        //  Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name(), user.isActive(), user.getRefreshTokenVersion()
        );

        // refreshToken gets set as cookie in the controller
        // we return it here so controller can set it
        return AuthResponse.builder()
                .accessToken(accessToken)
                .role(user.getRole().name())
                .authId(user.getId())
                .tokenType("Bearer")
                .build();
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        var claims = jwtUtil.validateAndExtract(refreshToken);
        String userId = claims.getSubject();
        long tokenVersion;
        try {
            tokenVersion = jwtUtil.extractRefreshTokenVersion(claims);
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        if (tokenVersion != user.getRefreshTokenVersion()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        return jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), user.getRole().name(), user.isActive(), user.getRefreshTokenVersion()
        );
    }

    public String generateRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            if (user.getRole() == User.Role.DOCTOR) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Account pending admin verification");
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        return jwtUtil.generateRefreshToken(user.getId(), user.getRefreshTokenVersion());
    }

    public void activateUser(String targetUserId, String adminAccessToken) {
        User admin = requireUserFromAccessToken(adminAccessToken);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        target.setActive(true);
        target.setActivatedAt(Instant.now());
        target.setActivatedBy(admin.getId());
        target.setDeactivatedAt(null);
        target.setDeactivatedBy(null);
        target.setDeactivationReason(null);
        userRepository.save(target);
    }

    public void deactivateUser(String targetUserId, String reason, String adminAccessToken) {
        User admin = requireUserFromAccessToken(adminAccessToken);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean wasActive = target.isActive();
        target.setActive(false);
        target.setDeactivatedAt(Instant.now());
        target.setDeactivatedBy(admin.getId());
        target.setDeactivationReason(reason);
        if (wasActive) {
            target.setRefreshTokenVersion(target.getRefreshTokenVersion() + 1);
        }
        userRepository.save(target);
    }

    public void deactivateSelf(String reason, String accessToken) {
        User actor = requireUserFromAccessToken(accessToken);
        if (actor.getRole() != User.Role.PATIENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only patients can self-deactivate");
        }

        boolean wasActive = actor.isActive();
        actor.setActive(false);
        actor.setDeactivatedAt(Instant.now());
        actor.setDeactivatedBy(actor.getId());
        actor.setDeactivationReason(reason);
        if (wasActive) {
            actor.setRefreshTokenVersion(actor.getRefreshTokenVersion() + 1);
        }
        userRepository.save(actor);
    }

    public void invalidateAllRefreshSessions(String accessToken) {
        User actor = requireUserFromAccessToken(accessToken);
        actor.setRefreshTokenVersion(actor.getRefreshTokenVersion() + 1);
        userRepository.save(actor);
    }

    private User requireUserFromAccessToken(String accessTokenHeader) {
        if (accessTokenHeader == null || !accessTokenHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = accessTokenHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }

        var claims = jwtUtil.validateAndExtract(token);
        String userId = claims.getSubject();
        long tokenVersion;
        try {
            tokenVersion = jwtUtil.extractTokenVersion(claims);
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        if (tokenVersion != user.getRefreshTokenVersion()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Access token has been revoked");
        }

        return user;
    }
}