package com.aferent.appointment_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    private String patientId;
    private String patientName;
    private String patientEmail;

    private String doctorId;
    private String doctorAuthId;   // authId from auth-service (for JWT-based queries)
    private String doctorName;

    // Reference to friend's schedule + your generated slot
    private String scheduleId;
    private String generatedSlotId;

    private AppointmentType type;
    private AppointmentStatus status;
    private LocalDate appointmentDate;

    // Physical specific
    private Integer appointmentNumber;
    private LocalTime calculatedTime;
    private String hospitalName;
    private String hospitalLocation;

    // Video specific
    private String videoSlotId;
    private LocalTime videoSlotStart;
    private LocalTime videoSlotEnd;
    private String videoSessionLink;

    // Payment
    private String paymentId;
    private double consultationFee;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
}