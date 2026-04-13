package com.aferent.appointment_service.model;

// model/GeneratedSlot.java
// These are slots YOUR service generates from the doctor's schedule template.
// Your friend stores the schedule pattern (Mon 4-6pm).
// You generate the actual dated slots (Mon Apr 7 4:00pm, Mon Apr 7 4:15pm...).

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "generated_slots")
public class GeneratedSlot {

    @Id
    private String id;

    private String scheduleId;      // ID from doctor friend's schedule
    private String doctorId;
    private AppointmentType type;   // PHYSICAL or VIDEO

    // For physical: this is one 15-min slot
    // For video: this is one 1-hour slot
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    // Physical only
    private Integer appointmentNumber;  // slot #1, #2...
    private String hospitalName;
    private String hospitalLocation;

    // Video only
    private String videoSlotId;     // e.g. "slot_001"

    private boolean booked;         // true when appointment confirmed/pending
    private double consultationFee;
}