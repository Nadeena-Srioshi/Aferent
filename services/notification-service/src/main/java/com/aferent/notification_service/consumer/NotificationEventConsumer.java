package com.aferent.notification_service.consumer;

import com.aferent.notification_service.dto.NotificationRequest;
import com.aferent.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    // ONE single topic for everything
    // Any service publishes here — we handle it
    @KafkaListener(topics = "notification.send", groupId = "notification-group")
    public void onNotificationRequest(NotificationRequest request) {
        log.info("Received notification request from [{}] | event: {} | channel: {}",
            request.getSourceService(),
            request.getEventType(),
            request.getChannel()
        );
        notificationService.process(request);
    }
}