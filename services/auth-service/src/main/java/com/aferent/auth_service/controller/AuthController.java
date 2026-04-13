package com.aferent.auth_service.controller;

import com.aferent.auth_service.dto.AuthResponse;
import com.aferent.auth_service.dto.LoginRequest;
import com.aferent.auth_service.dto.RegisterRequest;
import com.aferent.auth_service.exception.ApiException;
import com.aferent.auth_service.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);


        Cookie refreshCookie = new Cookie("refreshToken",
                authService.generateRefreshToken(request.getEmail()));
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/auth");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            HttpServletRequest request
    ) {
        // read refresh token from cookie
        String refreshToken = Arrays.stream(
                        request.getCookies() != null ? request.getCookies() : new Cookie[0]
                )
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No refresh token"));

        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletResponse response
    ) {
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletResponse response
    ) {
        authService.invalidateAllRefreshSessions(authorizationHeader);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

    @PostMapping("/admin/users/{userId}/activate")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        authService.activateUser(userId, authorizationHeader);
        return ResponseEntity.ok(Map.of("message", "User activated", "userId", userId));
    }

    @PostMapping("/admin/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "Deactivated by admin") String reason,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        authService.deactivateUser(userId, reason, authorizationHeader);
        return ResponseEntity.ok(Map.of("message", "User deactivated", "userId", userId));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateSelf(
            @RequestParam(defaultValue = "Deactivated by user") String reason,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletResponse response
    ) {
        authService.deactivateSelf(reason, authorizationHeader);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Account deactivated"));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}