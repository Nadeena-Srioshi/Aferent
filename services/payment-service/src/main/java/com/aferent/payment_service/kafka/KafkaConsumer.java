package com.aferent.payment_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aferent.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // Appointment service fires this when patient cancels and is eligible for refund
    @KafkaListener(topics = "refund.trigger", groupId = "payment-service")
    public void onRefundTrigger(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String appointmentId = node.get("appointmentId").asText();
            String paymentId     = node.get("paymentId").asText();

            log.info("Refund trigger received for appointmentId={} paymentId={}",
                    appointmentId, paymentId);

            paymentService.processRefundById(paymentId, appointmentId);

        } catch (Exception e) {
            log.error("Error processing refund.trigger: {}", e.getMessage());
        }
    }
}