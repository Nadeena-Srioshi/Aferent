package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.ScheduleOverrideRequest;
import com.aferent.doctor_service.dto.WeeklyScheduleRequest;
import com.aferent.doctor_service.model.ScheduleOverride;
import com.aferent.doctor_service.model.WeeklySchedule;
import com.aferent.doctor_service.service.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private final DoctorScheduleService scheduleService;

    // set or update full weekly schedule
    @PutMapping("/{doctorId}/schedule/weekly")
    public ResponseEntity<WeeklySchedule> setWeeklySchedule(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @RequestBody WeeklyScheduleRequest request
    ) {
        return ResponseEntity.ok(scheduleService.setWeeklySchedule(doctorId, authId, request));
    }

    // get own weekly schedule
    @GetMapping("/{doctorId}/schedule/weekly")
    public ResponseEntity<WeeklySchedule> getWeeklySchedule(
            @PathVariable String doctorId
    ) {
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(doctorId));
    }

    // add a date-specific override (block or add)
    @PostMapping("/{doctorId}/schedule/overrides")
    public ResponseEntity<ScheduleOverride> addOverride(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @Valid @RequestBody ScheduleOverrideRequest request
    ) {
        return ResponseEntity.ok(scheduleService.addOverride(doctorId, authId, request));
    }

    // get all upcoming overrides
    @GetMapping("/{doctorId}/schedule/overrides")
    public ResponseEntity<List<ScheduleOverride>> getOverrides(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId
    ) {
        return ResponseEntity.ok(scheduleService.getUpcomingOverrides(doctorId));
    }

    // delete a specific override
    @DeleteMapping("/{doctorId}/schedule/overrides/{overrideId}")
    public ResponseEntity<Void> deleteOverride(
            @PathVariable String doctorId,
            @PathVariable String overrideId,
            @RequestHeader("X-User-ID") String authId
    ) {
        scheduleService.deleteOverride(doctorId, authId, overrideId);
        return ResponseEntity.noContent().build();
    }
}