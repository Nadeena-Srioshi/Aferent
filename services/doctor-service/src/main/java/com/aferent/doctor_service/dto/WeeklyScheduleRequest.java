package com.aferent.doctor_service.dto;

import com.aferent.doctor_service.model.WeeklySchedule.DaySchedule;
import lombok.Data;

import java.util.List;

@Data
public class WeeklyScheduleRequest {
    private List<DaySchedule> monday;
    private List<DaySchedule> tuesday;
    private List<DaySchedule> wednesday;
    private List<DaySchedule> thursday;
    private List<DaySchedule> friday;
    private List<DaySchedule> saturday;
    private List<DaySchedule> sunday;
}