package com.aferent.appointment_service.dto;

// dto/SlotResponse.java
// What the patient sees when browsing available slots\
import com.aferent.appointment_service.model.AppointmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class SlotResponse {
    private String slotId;              // generatedSlot ID
    private String scheduleId;
    private String doctorId;
    private AppointmentType type;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean booked;
    private double consultationFee;

    // Physical only
    private Integer appointmentNumber;
    private String hospitalName;
    private String hospitalLocation;

    // Video only
    private String videoSlotId;
}