package com.aferent.doctor_service.dto;

import com.aferent.doctor_service.model.ScheduleOverride.OverrideAction;
import com.aferent.doctor_service.model.WeeklySchedule.DaySchedule;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleOverrideRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Action is required")
    private OverrideAction action;

    private List<DaySchedule> slots; // for ADD
    private String sessionId;        // for CANCEL_SESSION
    private String reason;
}