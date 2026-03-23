package com.aferent.auth_service.service;

import com.aferent.auth_service.repository.UserRepository;
import com.aferent.auth_service.dto.AuthResponse;
import com.aferent.auth_service.dto.LoginRequest;
import com.aferent.auth_service.dto.RegisterRequest;
import com.aferent.auth_service.model.User;
import com.aferent.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {

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

        //  Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                saved.getId(), saved.getEmail(), saved.getRole().name()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .role(saved.getRole().name())
                .userId(saved.getId())
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
                .userId(user.getId())
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