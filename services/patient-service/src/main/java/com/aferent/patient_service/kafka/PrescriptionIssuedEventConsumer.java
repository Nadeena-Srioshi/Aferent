package com.aferent.patient_service.kafka;

import com.aferent.patient_service.service.MedicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionIssuedEventConsumer {

    private final MedicalHistoryService medicalHistoryService;

    /**
     * Listens to prescription.issued topic from doctor-service
     * and records the prescription in the patient's medical history
     */
    @KafkaListener(topics = "prescription.issued", groupId = "patient-service-prescription-group")
    public void onPrescriptionIssued(ConsumerRecord<String, Map<String, Object>> record) {
        try {
            Map<String, Object> event = record.value();

            String patientId = (String) event.get("patientId");
            if (patientId == null || patientId.isBlank()) {
                log.warn("Received prescription.issued event with missing patientId: {}", event);
                return;
            }

            // Extract prescription data
            String prescriptionId = (String) event.get("prescriptionId");
            String appointmentId = (String) event.get("appointmentId");
            String consultationType = (String) event.get("consultationType");

            String doctorId = (String) event.get("doctorId");
            String doctorName = (String) event.get("doctorName");
            String doctorSpecialization = (String) event.get("doctorSpecialization");

            String hospitalId = (String) event.get("hospitalId");
            String hospitalName = (String) event.get("hospitalName");

            String patientName = (String) event.get("patientName");
            Integer patientAge = event.get("patientAge") != null ? 
                ((Number) event.get("patientAge")).intValue() : null;
            String patientPhone = (String) event.get("patientPhone");
            String patientEmail = (String) event.get("patientEmail");

            String diagnosis = (String) event.get("diagnosis");
            List<String> symptoms = normalizeSymptoms(event.get("symptoms"), event.get("symptomsText"));
            List<Map<String, Object>> medications = normalizeMedications(event.get("medications"));
            String notes = (String) event.get("notes");
            String followUpDate = normalizeLocalDate(event.get("followUpDate"));

            String qrCodeKey = (String) event.get("qrCodeKey");
            String issuedAt = normalizeLocalDateTime(event.get("issuedAt"));

            // Record prescription in medical history
            medicalHistoryService.recordPrescription(
                patientId,
                prescriptionId,
                appointmentId,
                consultationType,
                doctorId,
                doctorName,
                doctorSpecialization,
                hospitalId,
                hospitalName,
                patientName,
                patientAge,
                patientPhone,
                patientEmail,
                diagnosis,
                symptoms,
                medications,
                notes,
                followUpDate,
                qrCodeKey,
                issuedAt
            );

            log.info("Recorded prescription prescriptionId={} for patientId={} from Kafka event",
                prescriptionId, patientId);

        } catch (Exception e) {
            log.error("Error processing prescription.issued event: {}", record.value(), e);
        }
    }

    private List<String> normalizeSymptoms(Object symptomsRaw, Object symptomsTextRaw) {
        if (symptomsRaw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        String text = null;
        if (symptomsRaw instanceof String s && !s.isBlank()) {
            text = s;
        } else if (symptomsTextRaw instanceof String s2 && !s2.isBlank()) {
            text = s2;
        }

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return List.of(text.trim());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeMedications(Object medicationsRaw) {
        if (!(medicationsRaw instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String normalizeLocalDate(Object raw) {
        if (raw == null) {
            return null;
        }

        if (raw instanceof String s) {
            return s;
        }

        if (raw instanceof List<?> list && list.size() >= 3) {
            Integer y = asInt(list.get(0));
            Integer m = asInt(list.get(1));
            Integer d = asInt(list.get(2));
            if (y != null && m != null && d != null) {
                return LocalDate.of(y, m, d).toString();
            }
        }

        return String.valueOf(raw);
    }

    private String normalizeLocalDateTime(Object raw) {
        if (raw == null) {
            return null;
        }

        if (raw instanceof String s) {
            return s;
        }

        if (raw instanceof List<?> list && list.size() >= 6) {
            Integer y = asInt(list.get(0));
            Integer m = asInt(list.get(1));
            Integer d = asInt(list.get(2));
            Integer hh = asInt(list.get(3));
            Integer mm = asInt(list.get(4));
            Integer ss = asInt(list.get(5));
            Integer nanos = list.size() > 6 ? asInt(list.get(6)) : 0;

            if (y != null && m != null && d != null && hh != null && mm != null && ss != null) {
                return LocalDateTime.of(y, m, d, hh, mm, ss, nanos != null ? nanos : 0).toString();
            }
        }

        return String.valueOf(raw);
    }

    private Integer asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
