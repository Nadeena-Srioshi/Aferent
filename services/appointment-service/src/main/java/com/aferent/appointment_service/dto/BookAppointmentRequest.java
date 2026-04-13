package com.aferent.appointment_service.dto;


import com.aferent.appointment_service.model.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookAppointmentRequest {

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotBlank(message = "Schedule ID is required")
    private String scheduleId;          // from friend's doctor service

    @NotNull(message = "Type is required")
    private AppointmentType type;

    @NotBlank(message = "Appointment date is required")
    private String appointmentDate;     // "2025-04-07"

    private String patientName;
    private String doctorName;

    // Video only — patient picks which slot
    private String videoSlotId;         // "slot_001"

    // Physical only — leave null, system assigns next available number
    // appointmentNumber is assigned automatically by the service
}
