// package com.aferent.doctor_service.kafka;

// import com.aferent.doctor_service.model.Prescription;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.HashMap;
// import java.util.Map;

// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class DoctorEventProducer {

//     private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

//     @Value("${kafka-topics.prescription-issued:prescription.issued}")
//     private String prescriptionIssuedTopic;

//     public void sendNotification(Map<String, Object> payload) {
//         kafkaTemplate.send("notification.send", payload);
//         log.info("Published to notification.send channel={}", payload.get("channel"));
//     }

//     public void sendVerificationEvent(String authId, String action) {
//         Map<String, Object> payload = new HashMap<>();
//         payload.put("authId", authId);
//         payload.put("action", action); // "APPROVE" or "REJECT"
//         kafkaTemplate.send("doctor.verification.result", authId, payload);
//         log.info("Published doctor.verification.result authId={} action={}", authId, action);
//     }

//     public void sendPrescriptionIssuedEvent(Prescription prescription) {
//         Map<String, Object> payload = new HashMap<>();
//         payload.put("eventType", "prescription.issued");
//         payload.put("version", 1);
//         payload.put("sourceService", "doctor-service");
//         payload.put("publishedAt", LocalDateTime.now().toString());

//         payload.put("prescriptionId", prescription.getPrescriptionId());
//         payload.put("appointmentId", prescription.getAppointmentId());
//         payload.put("consultationType", prescription.getConsultationType());

//         payload.put("doctorId", prescription.getDoctorId());
//         payload.put("doctorName", prescription.getDoctorName());
//         payload.put("doctorSpecialization", prescription.getDoctorSpecialization());

//         payload.put("hospitalId", prescription.getHospitalId());
//         payload.put("hospitalName", prescription.getHospitalName());

//         payload.put("patientId", prescription.getPatientId());
//         payload.put("patientName", prescription.getPatientName());
//         payload.put("patientAge", prescription.getPatientAge());
//         payload.put("patientPhone", prescription.getPatientPhone());
//         payload.put("patientEmail", prescription.getPatientEmail());

//         payload.put("diagnosis", prescription.getDiagnosis());
//         payload.put("symptoms", prescription.getSymptoms());
//         payload.put("medications", prescription.getMedications());
//         payload.put("notes", prescription.getNotes());
//         payload.put("followUpDate", prescription.getFollowUpDate());

//         payload.put("qrCodeKey", prescription.getQrCodeKey());
//         payload.put("issuedAt", prescription.getIssuedAt());

//         String key = (prescription.getPatientId() != null && !prescription.getPatientId().isBlank())
//                 ? prescription.getPatientId()
//                 : prescription.getPrescriptionId();

//         kafkaTemplate.send(prescriptionIssuedTopic, key, payload);
//         log.info("Published {} prescriptionId={} patientId={}",
//                 prescriptionIssuedTopic,
//                 prescription.getPrescriptionId(),
//                 prescription.getPatientId());
//     }
// }

package com.aferent.doctor_service.kafka;

import com.aferent.doctor_service.model.Prescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventProducer {

    public static final String WEEKLY_UPSERTED_TOPIC = "doctor.schedule.weekly.upserted";
    public static final String WEEKLY_DELETED_TOPIC = "doctor.schedule.weekly.deleted";
    public static final String OVERRIDE_UPSERTED_TOPIC = "doctor.schedule.override.upserted";
    public static final String OVERRIDE_DELETED_TOPIC = "doctor.schedule.override.deleted";

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${kafka-topics.prescription-issued:prescription.issued}")
    private String prescriptionIssuedTopic;


    public void sendNotification(Map<String, Object> payload) {
        kafkaTemplate.send("notification.send", payload);
        log.info("Published to notification.send channel={}", payload.get("channel"));
    }

    public void sendVerificationEvent(String authId, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("authId", authId);
        payload.put("action", action); // "APPROVE" or "REJECT"
        kafkaTemplate.send("doctor.verification.result", authId, payload);
        log.info("Published doctor.verification.result authId={} action={}", authId, action);
    }

    public void publishWeeklyUpserted(String doctorId,
                                      String scheduleId,
                                      String dayOfWeek,
                                      String startTime,
                                      String endTime,
                                      String type,
                                      String hospital) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "WEEKLY_UPSERT");
        payload.put("id", scheduleId);
        payload.put("doctorId", doctorId);

        // For full-refresh trigger events, schedule detail fields may be absent.
        // Only include optional fields when we actually have values.
        if (dayOfWeek != null && !dayOfWeek.isBlank()) {
            payload.put("dayOfWeek", dayOfWeek);
        }
        if (startTime != null && !startTime.isBlank()) {
            payload.put("startTime", startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            payload.put("endTime", endTime);
        }
        if (type != null && !type.isBlank()) {
            payload.put("type", type);
        }
        if (hospital != null && !hospital.isBlank()) {
            payload.put("hospitalName", hospital);
        }

        if (!payload.containsKey("dayOfWeek")
                && !payload.containsKey("startTime")
                && !payload.containsKey("endTime")
                && !payload.containsKey("type")) {
            payload.put("mode", "FULL_REFRESH_TRIGGER");
        }

        payload.put("active", true);
        safeSend(WEEKLY_UPSERTED_TOPIC, doctorId, payload);
    }

    public void publishWeeklyDeleted(String doctorId, String scheduleId) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "WEEKLY_DELETE");
        payload.put("id", scheduleId);
        payload.put("doctorId", doctorId);
        payload.put("active", false);
        safeSend(WEEKLY_DELETED_TOPIC, doctorId, payload);
    }

    public void publishOverrideUpsertedBlock(String doctorId, LocalDate date) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "OVERRIDE_UPSERT");
        payload.put("overrideAction", "BLOCK");
        payload.put("doctorId", doctorId);
        payload.put("date", date.toString());
        safeSend(OVERRIDE_UPSERTED_TOPIC, doctorId, payload);
    }

    public void publishOverrideUpsertedAdd(String doctorId,
                                           String scheduleId,
                                           LocalDate date,
                                           String startTime,
                                           String endTime,
                                           String type,
                                           String hospital,
                                           Double consultationFee) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "OVERRIDE_UPSERT");
        payload.put("overrideAction", "ADD");
        payload.put("doctorId", doctorId);
        payload.put("date", date.toString());
        payload.put("id", scheduleId);
        payload.put("dayOfWeek", date.getDayOfWeek().name());
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        payload.put("type", type);
        payload.put("hospitalName", hospital);
        payload.put("consultationFee", consultationFee != null ? consultationFee : 0.0);
        payload.put("active", true);
        safeSend(OVERRIDE_UPSERTED_TOPIC, doctorId, payload);
    }

    public void publishOverrideUpsertedCancelSession(String doctorId,
                                                     LocalDate date,
                                                     String sessionId) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "OVERRIDE_UPSERT");
        payload.put("overrideAction", "CANCEL_SESSION");
        payload.put("doctorId", doctorId);
        payload.put("date", date.toString());
        payload.put("sessionId", sessionId);
        safeSend(OVERRIDE_UPSERTED_TOPIC, doctorId, payload);
    }

    public void publishOverrideDeleted(String doctorId,
                                       String deletedAction,
                                       LocalDate date,
                                       String scheduleId,
                                       String sessionId,
                                       List<Map<String, Object>> restoreSchedules) {
        Map<String, Object> payload = basePayload(doctorId);
        payload.put("eventType", "OVERRIDE_DELETE");
        payload.put("deletedAction", deletedAction);
        payload.put("doctorId", doctorId);
        payload.put("date", date.toString());

        if (scheduleId != null) {
            payload.put("id", scheduleId);
        }

        if (sessionId != null) {
            payload.put("sessionId", sessionId);
        }

        if (restoreSchedules != null && !restoreSchedules.isEmpty()) {
            payload.put("restoreSchedules", restoreSchedules);
        }

        safeSend(OVERRIDE_DELETED_TOPIC, doctorId, payload);
    }

    public List<Map<String, Object>> buildRestoreSchedules(List<Map<String, Object>> schedules) {
        return schedules == null ? new ArrayList<>() : schedules;
    }

    private Map<String, Object> basePayload(String doctorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("occurredAt", LocalDateTime.now().toString());
        payload.put("doctorId", doctorId);
        return payload;
    }

    private void safeSend(String topic, String key, Map<String, Object> payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.info("Published {} eventType={} key={}", topic, payload.get("eventType"), key);
        } catch (Exception e) {
            log.error("Failed to publish topic={} key={} reason={}", topic, key, e.getMessage());
        }
    }

    public void sendPrescriptionIssuedEvent(Prescription prescription) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "prescription.issued");
        payload.put("version", 1);
        payload.put("sourceService", "doctor-service");
        payload.put("publishedAt", LocalDateTime.now().toString());

        payload.put("prescriptionId", prescription.getPrescriptionId());
        payload.put("appointmentId", prescription.getAppointmentId());
        payload.put("consultationType", prescription.getConsultationType());

        payload.put("doctorId", prescription.getDoctorId());
        payload.put("doctorName", prescription.getDoctorName());
        payload.put("doctorSpecialization", prescription.getDoctorSpecialization());

        payload.put("hospitalId", prescription.getHospitalId());
        payload.put("hospitalName", prescription.getHospitalName());

        payload.put("patientId", prescription.getPatientId());
        payload.put("patientName", prescription.getPatientName());
        payload.put("patientAge", prescription.getPatientAge());
        payload.put("patientPhone", prescription.getPatientPhone());
        payload.put("patientEmail", prescription.getPatientEmail());

        payload.put("diagnosis", prescription.getDiagnosis());
        payload.put("symptoms", normalizeSymptoms(prescription.getSymptoms()));
        payload.put("symptomsText", prescription.getSymptoms());
        payload.put("medications", prescription.getMedications());
        payload.put("notes", prescription.getNotes());
        payload.put("followUpDate", prescription.getFollowUpDate() != null ? prescription.getFollowUpDate().toString() : null);

        payload.put("qrCodeKey", prescription.getQrCodeKey());
        payload.put("issuedAt", prescription.getIssuedAt() != null ? prescription.getIssuedAt().toString() : null);

        String key = (prescription.getPatientId() != null && !prescription.getPatientId().isBlank())
                ? prescription.getPatientId()
                : prescription.getPrescriptionId();

        kafkaTemplate.send(prescriptionIssuedTopic, key, payload);
        log.info("Published {} prescriptionId={} patientId={}",
                prescriptionIssuedTopic,
                prescription.getPrescriptionId(),
                prescription.getPatientId());
    }

    private List<String> normalizeSymptoms(String symptoms) {
        if (symptoms == null || symptoms.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(symptoms.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}