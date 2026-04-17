package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "weekly_schedules")
public class WeeklySchedule {

    @Id
    private String id;

    @Indexed(unique = true)
    private String doctorId;

    private List<DaySchedule> monday;
    private List<DaySchedule> tuesday;
    private List<DaySchedule> wednesday;
    private List<DaySchedule> thursday;
    private List<DaySchedule> friday;
    private List<DaySchedule> saturday;
    private List<DaySchedule> sunday;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {

        private String sessionId; // unique per session
        private String startTime;
        private String endTime;
        private String hospital;
        private SessionType type;

        public enum SessionType {
            IN_PERSON, VIDEO
        }
    }
}