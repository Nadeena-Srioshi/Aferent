package com.aferent.doctor_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventProducer {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    public void sendNotification(Map<String, Object> payload) {
        kafkaTemplate.send("notification.send", payload);
        log.info("Published to notification.send channel={}", payload.get("channel"));
    }
}