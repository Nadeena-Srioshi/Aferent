package com.aferent.patient_service.kafka;

import com.aferent.patient_service.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventConsumer {

    private final PatientService patientService;

    // listens to user.registered topic published by auth-service
    @KafkaListener(topics = "user.registered", groupId = "patient-service-group")
    public void onUserRegistered(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        String role = (String) event.get("role");

        // only create patient profiles for PATIENT role users
        if (!"PATIENT".equals(role)) {
            return;
        }

        String authId = (String) event.get("authId");
        if (authId == null || authId.isBlank()) {
            authId = (String) event.get("userId"); // backward compatibility
        }
        String email  = (String) event.get("email");

        log.info("Received user.registered for patient authId={}", authId);
        patientService.createProfile(authId, email);
    }
}