package com.aferent.appointment_service.dto;

// dto/DoctorScheduleDto.java
// Maps the JSON your friend's Doctor Service returns for a schedule entry
import lombok.Data;

@Data
public class DoctorScheduleDto {
    private String id;              // scheduleId from doctor service
    private String doctorId;
    private String doctorName;
    private String type;            // "PHYSICAL" or "VIDEO"
    private String dayOfWeek;       // "MONDAY"
    private String startTime;       // "16:00"
    private String endTime;         // "18:00"
    private String hospitalName;
    private String hospitalAddress;
    private Integer sessionDurationMinutes;
    private double consultationFee;
    private boolean active;
}