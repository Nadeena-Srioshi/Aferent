package com.aferent.notification_service.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "notification_logs")
public class NotificationLog {

    @Id
    private String id;

    private String sourceService;   // which service sent this
    private String eventType;       // what event triggered it
    private String recipient;       // phone or email
    private String channel;         // SMS or EMAIL
    private String status;          // SENT or FAILED
    private String message;         // message content
    private LocalDateTime sentAt;
}