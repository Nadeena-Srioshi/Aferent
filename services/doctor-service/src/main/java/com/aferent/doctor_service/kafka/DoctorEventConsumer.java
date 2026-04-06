package com.aferent.doctor_service.kafka;

import com.aferent.doctor_service.service.DoctorRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventConsumer {

    private final DoctorRegistrationService doctorRegistrationService;

    @KafkaListener(topics = "user.registered", groupId = "doctor-service-group")
    public void onUserRegistered(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        String role = (String) event.get("role");

        // only handle DOCTOR role
        if (!"DOCTOR".equals(role)) {
            return;
        }

        String authId = (String) event.get("authId");
        String email  = (String) event.get("email");

        log.info("Received user.registered for doctor authId={}", authId);
        doctorRegistrationService.createPendingProfile(authId, email);
    }
}