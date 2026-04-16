package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "schedule_overrides")
public class ScheduleOverride {

    @Id
    private String id;

    @Indexed
    private String doctorId;

    private LocalDate date;
    private OverrideAction action;

    private List<WeeklySchedule.DaySchedule> slots; // for ADD
    private String sessionId; // for CANCEL_SESSION
    private String reason;

    public enum OverrideAction {
        BLOCK,
        ADD,
        CANCEL_SESSION
    }

    @CreatedDate
    private LocalDateTime createdAt;
}