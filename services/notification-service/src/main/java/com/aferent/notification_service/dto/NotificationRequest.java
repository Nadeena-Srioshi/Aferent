package com.aferent.notification_service.dto;

import com.aferent.notification_service.enums.NotificationChannel;
import lombok.Data;

@Data
public class NotificationRequest {

    private String sourceService;       // e.g. "appointment-service"

    private String eventType;           // e.g. "appointment.booked"

    private NotificationChannel channel; // SMS, EMAIL, or BOTH

    private String toPhone;             // required if channel is SMS or BOTH
    private String toEmail;             // required if channel is EMAIL or BOTH

    private String subject;             // email subject (only for EMAIL or BOTH)
    private String message;             // the actual message body (SMS + Email)
}
