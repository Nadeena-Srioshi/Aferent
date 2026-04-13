package com.aferent.notification_service.service;

import com.aferent.notification_service.dto.NotificationRequest;
import com.aferent.notification_service.model.NotificationLog;
import com.aferent.notification_service.provider.EmailProvider;
import com.aferent.notification_service.provider.SmsProvider;
import com.aferent.notification_service.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;
    private final NotificationLogRepository logRepository;

    public void process(NotificationRequest request) {
        switch (request.getChannel()) {
            case SMS  -> sendSms(request);
            case EMAIL -> sendEmail(request);
            case BOTH  -> {
                sendSms(request);
                sendEmail(request);
            }
            default -> log.warn("Unknown channel: {}", request.getChannel());
        }
    }

    private void sendSms(NotificationRequest request) {
        boolean success = smsProvider.sendSms(
            request.getToPhone(),
            request.getMessage()
        );
        saveLog(request, "SMS", success);
    }

    private void sendEmail(NotificationRequest request) {
        boolean success = emailProvider.sendEmail(
            request.getToEmail(),
            request.getSubject(),
            request.getMessage()
        );
        saveLog(request, "EMAIL", success);
    }

    private void saveLog(NotificationRequest request, 
                         String channel, boolean success) {
        logRepository.save(NotificationLog.builder()
            .sourceService(request.getSourceService())
            .eventType(request.getEventType())
            .recipient(channel.equals("SMS") 
                ? request.getToPhone() 
                : request.getToEmail())
            .channel(channel)
            .status(success ? "SENT" : "FAILED")
            .message(request.getMessage())
            .sentAt(LocalDateTime.now())
            .build());
    }
}