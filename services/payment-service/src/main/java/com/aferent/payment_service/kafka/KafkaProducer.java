package com.aferent.payment_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aferent.payment_service.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Fires when Stripe webhook confirms payment_intent.succeeded
    public void publishPaymentSuccess(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId",             payment.getId());
        payload.put("appointmentId",         payment.getAppointmentId());
        payload.put("patientId",             payment.getPatientId());
        payload.put("patientEmail",          payment.getPatientEmail());
        payload.put("doctorId",              payment.getDoctorId());
        payload.put("amount",                payment.getAmount());
        payload.put("currency",              payment.getCurrency());
        payload.put("stripePaymentIntentId", payment.getStripePaymentIntentId());
        payload.put("appointmentType",       payment.getAppointmentType());
        publish("payment.success", payload);
    }

    // Fires when Stripe webhook reports payment_intent.payment_failed
    public void publishPaymentFailed(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId",     payment.getId());
        payload.put("appointmentId", payment.getAppointmentId());
        payload.put("patientId",     payment.getPatientId());
        payload.put("patientEmail",  payment.getPatientEmail());
        payload.put("doctorId",      payment.getDoctorId());
        payload.put("amount",        payment.getAmount());
        publish("payment.failed", payload);
    }

    // Fires when refund is successfully processed with Stripe
    public void publishRefundCompleted(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId",      payment.getId());
        payload.put("appointmentId",  payment.getAppointmentId());
        payload.put("patientId",      payment.getPatientId());
        payload.put("patientEmail",   payment.getPatientEmail());
        payload.put("amount",         payment.getAmount());
        payload.put("stripeRefundId", payment.getStripeRefundId());
        publish("refund.completed", payload);
    }

    private void publish(String topic, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
            log.info("Published to {}: {}", topic, json);
        } catch (Exception e) {
            log.error("Failed to publish to {}: {}", topic, e.getMessage());
        }
    }
}