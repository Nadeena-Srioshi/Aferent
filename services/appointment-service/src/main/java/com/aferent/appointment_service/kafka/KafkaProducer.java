package com.aferent.appointment_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aferent.appointment_service.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Spring automatically injects both beans through this constructor
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAppointmentBooked(Appointment a) {
        Map<String, Object> p = base(a);
        p.put("consultationFee", a.getConsultationFee());
        p.put("hospitalName", a.getHospitalName());
        p.put("videoSlotStart", a.getVideoSlotStart() != null ? a.getVideoSlotStart().toString() : null);
        p.put("videoSlotEnd", a.getVideoSlotEnd() != null ? a.getVideoSlotEnd().toString() : null);
        publish("appointment.booked", p);
    }

    public void publishAppointmentCancelled(Appointment a) {
        publish("appointment.cancelled", base(a));
    }

    public void publishRefundTrigger(Appointment a) {
        publish("refund.trigger", Map.of(
                "appointmentId", a.getId(),
                "patientId", a.getPatientId(),
                "paymentId", a.getPaymentId() != null ? a.getPaymentId() : "",
                "amount", a.getConsultationFee()
        ));
    }

    public void publishSlotBooked(Appointment a) {
        publish("slot.booked", Map.of(
                "appointmentId", a.getId(),
                "doctorId", a.getDoctorId(),
                "scheduleId", a.getScheduleId(),
                "videoSlotId", a.getVideoSlotId() != null ? a.getVideoSlotId() : "",
                "appointmentDate", a.getAppointmentDate().toString()
        ));
    }

    public void publishAppointmentConfirmed(Appointment a) {
        Map<String, Object> p = base(a);
        p.put("videoSessionLink", a.getVideoSessionLink());
        p.put("calculatedTime", a.getCalculatedTime() != null ? a.getCalculatedTime().toString() : null);
        p.put("videoSlotStart", a.getVideoSlotStart() != null ? a.getVideoSlotStart().toString() : null);
        publish("appointment.confirmed", p);
    }

    private Map<String, Object> base(Appointment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("appointmentId", a.getId());
        m.put("patientId", a.getPatientId());
        m.put("patientEmail", a.getPatientEmail());
        m.put("doctorId", a.getDoctorId());
        m.put("doctorName", a.getDoctorName());
        m.put("type", a.getType().name());
        m.put("status", a.getStatus().name());
        m.put("appointmentDate", a.getAppointmentDate().toString());
        return m;
    }

    private void publish(String topic, Map<String, ?> payload) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload));
            log.info("Kafka → {}", topic);
        } catch (Exception e) {
            log.error("Kafka publish failed topic={} err={}", topic, e.getMessage());
        }
    }
}