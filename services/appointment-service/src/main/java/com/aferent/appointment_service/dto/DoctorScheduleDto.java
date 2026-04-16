package com.aferent.appointment_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class DoctorScheduleDto {
    private String id;              // overall schedule ID (e.g., "69e0006a4fd219a9dc42c7ad")
    private String doctorId;
    
    // Weekly structure - each day can have multiple sessions
    private List<SessionDto> monday;
    private List<SessionDto> tuesday;
    private List<SessionDto> wednesday;
    private List<SessionDto> thursday;
    private List<SessionDto> friday;
    private List<SessionDto> saturday;
    private List<SessionDto> sunday;
    
    private String updatedAt;
    
    @Data
    public static class SessionDto {
        private String sessionId;
        private String startTime;      // "08:00"
        private String endTime;        // "12:00"
        private String hospital;       // hospital ID or null
        private String type;           // "IN_PERSON" or "VIDEO"
        
        // Additional fields you might need
        private String hospitalName;
        private String hospitalAddress;
        private Integer sessionDurationMinutes;
        private Double consultationFee;
    }
}