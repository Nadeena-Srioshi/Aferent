package com.aferent.auth_service.service;

import com.aferent.auth_service.repository.UserRepository;
import com.aferent.auth_service.dto.AuthResponse;
import com.aferent.auth_service.dto.LoginRequest;
import com.aferent.auth_service.dto.RegisterRequest;
import com.aferent.auth_service.model.User;
import com.aferent.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
            throw new RuntimeException("Email already registered");
        }

        // Build the User document
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // bcrypt hash
                .role(User.Role.valueOf(request.getRole().toUpperCase()))
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
                saved.getId(), saved.getEmail(), saved.getRole().name()
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
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password against hash — NEVER compare plain text
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
            // same message intentionally — don't reveal which field is wrong
        }

        //  Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

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
            throw new RuntimeException("Invalid or expired refresh token");
        }
        var claims = jwtUtil.validateAndExtract(refreshToken);
        String userId = claims.getSubject();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );
    }

    public String generateRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jwtUtil.generateRefreshToken(user.getId());
    }
}