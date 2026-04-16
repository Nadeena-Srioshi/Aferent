package com.aferent.auth_service.consumer;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aferent.auth_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorVerificationConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "doctor.verification.result", groupId = "auth-service-group")
    public void onDoctorVerified(Map<String, Object> event) {
        String authId = event != null ? (String) event.get("authId") : null;
        String action = event != null ? (String) event.get("action") : null;
        log.info("Received doctor.verification.result event authId={} action={}", authId, action);

        if (authId == null || action == null) {
            log.warn("Ignoring doctor.verification.result due to missing authId/action payload={}", event);
            return;
        }

        userRepository.findById(authId).ifPresentOrElse(user -> {
            if ("APPROVE".equals(action)) {
                user.setActive(true);
                user.setActivatedAt(Instant.now());
            } else if ("REJECT".equals(action)) {
                user.setActive(false);
                user.setDeactivatedAt(Instant.now());
                user.setDeactivationReason("Doctor verification rejected");
                user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
            } else {
                log.warn("Ignoring doctor.verification.result with unsupported action={} authId={}", action, authId);
                return;
            }
            userRepository.save(user);
            log.info("Updated auth User.active for authId={} action={}", authId, action);
            
        }, () -> log.warn("No auth user found for authId={}", authId));
    }
}