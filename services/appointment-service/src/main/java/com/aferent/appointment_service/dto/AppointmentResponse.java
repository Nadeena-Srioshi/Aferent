package com.aferent.appointment_service.dto;

import com.aferent.appointment_service.model.AppointmentStatus;
import com.aferent.appointment_service.model.AppointmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentResponse {
    private String id;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorAuthId;
    private String doctorName;
    private AppointmentType type;
    private AppointmentStatus status;
    private LocalDate appointmentDate;

    // Physical
    private Integer appointmentNumber;
    private LocalTime calculatedTime;
    private String hospitalName;
    private String hospitalLocation;

    // Video
    private String videoSlotId;
    private LocalTime videoSlotStart;
    private LocalTime videoSlotEnd;
    private String videoSessionLink;

    private double consultationFee;
    private String paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
