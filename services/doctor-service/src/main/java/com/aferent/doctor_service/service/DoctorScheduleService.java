package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.ScheduleOverrideRequest;
import com.aferent.doctor_service.dto.WeeklyScheduleRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.ScheduleOverride;
import com.aferent.doctor_service.model.WeeklySchedule;
import com.aferent.doctor_service.model.WeeklySchedule.DaySchedule;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.ScheduleOverrideRepository;
import com.aferent.doctor_service.repository.WeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorScheduleService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final DoctorRepository doctorRepository;

    // ───────────────── WEEKLY SCHEDULE ─────────────────

    public WeeklySchedule setWeeklySchedule(String doctorId, String authId,
                                            WeeklyScheduleRequest request) {

        validateOwnership(doctorId, authId);
        validateAllDays(request);

        WeeklySchedule schedule = weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElse(WeeklySchedule.builder().doctorId(doctorId).build());

        schedule.setMonday(assignSessionIds(request.getMonday()));
        schedule.setTuesday(assignSessionIds(request.getTuesday()));
        schedule.setWednesday(assignSessionIds(request.getWednesday()));
        schedule.setThursday(assignSessionIds(request.getThursday()));
        schedule.setFriday(assignSessionIds(request.getFriday()));
        schedule.setSaturday(assignSessionIds(request.getSaturday()));
        schedule.setSunday(assignSessionIds(request.getSunday()));

        return weeklyScheduleRepository.save(schedule);
    }

    public WeeklySchedule getWeeklySchedule(String doctorId) {
        return weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("No schedule found"));
    }

    // ───────────────── OVERRIDES ─────────────────

    public ScheduleOverride addOverride(String doctorId, String authId,
                                        ScheduleOverrideRequest request) {

        validateOwnership(doctorId, authId);

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot modify past dates");
        }

        switch (request.getAction()) {

            case ADD -> {
                if (request.getSlots() == null || request.getSlots().isEmpty()) {
                    throw new IllegalArgumentException("Slots required for ADD");
                }
                validateSlotList(request.getSlots());
            }

            case CANCEL_SESSION -> {
                if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                    throw new IllegalArgumentException("sessionId required");
                }
                validateSessionExists(doctorId, request.getDate(), request.getSessionId());
            }

            case BLOCK -> {
                // nothing extra
            }
        }

        ScheduleOverride override = scheduleOverrideRepository
                .findByDoctorIdAndDate(doctorId, request.getDate())
                .orElse(ScheduleOverride.builder().doctorId(doctorId).build());

        override.setDate(request.getDate());
        override.setAction(request.getAction());
        override.setReason(request.getReason());

        if (request.getAction() == ScheduleOverride.OverrideAction.ADD) {
            override.setSlots(assignSessionIds(request.getSlots()));
            override.setSessionId(null);
        } else if (request.getAction() == ScheduleOverride.OverrideAction.CANCEL_SESSION) {
            override.setSessionId(request.getSessionId());
            override.setSlots(null);
        } else {
            override.setSlots(null);
            override.setSessionId(null);
        }

        return scheduleOverrideRepository.save(override);
    }

    public List<ScheduleOverride> getUpcomingOverrides(String doctorId) {
        return scheduleOverrideRepository
                .findByDoctorIdAndDateGreaterThanEqual(doctorId, LocalDate.now());
    }

    public void deleteOverride(String doctorId, String authId, String overrideId) {

        validateOwnership(doctorId, authId);

        ScheduleOverride override = scheduleOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new ResourceNotFoundException("Override not found"));

        if (!override.getDoctorId().equals(doctorId)) {
            throw new ForbiddenOperationException("Not your override");
        }

        scheduleOverrideRepository.deleteById(overrideId);
    }

    // ───────────────── VALIDATIONS ─────────────────

    private void validateAllDays(WeeklyScheduleRequest request) {
        validateSlotList(request.getMonday());
        validateSlotList(request.getTuesday());
        validateSlotList(request.getWednesday());
        validateSlotList(request.getThursday());
        validateSlotList(request.getFriday());
        validateSlotList(request.getSaturday());
        validateSlotList(request.getSunday());
    }

    private void validateSlotList(List<DaySchedule> slots) {
        if (slots == null || slots.isEmpty()) return;

        for (DaySchedule slot : slots) {
            validateSingleSlot(slot);
        }

        // overlap check
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (isOverlap(slots.get(i), slots.get(j))) {
                    throw new IllegalArgumentException("Overlapping sessions detected");
                }
            }
        }
    }

    private void validateSingleSlot(DaySchedule slot) {

        if (slot.getStartTime() == null || slot.getEndTime() == null) {
            throw new IllegalArgumentException("Time required");
        }

        LocalTime start;
        LocalTime end;

        try {
            start = LocalTime.parse(slot.getStartTime());
            end = LocalTime.parse(slot.getEndTime());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format (HH:mm)");
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        if (slot.getType() == null) {
            throw new IllegalArgumentException("Session type required");
        }

        if (slot.getType() == DaySchedule.SessionType.IN_PERSON &&
                (slot.getHospital() == null || slot.getHospital().isBlank())) {
            throw new IllegalArgumentException("Hospital required for IN_PERSON");
        }

        if (slot.getType() == DaySchedule.SessionType.VIDEO &&
                slot.getHospital() != null) {
            throw new IllegalArgumentException("VIDEO session cannot have hospital");
        }
    }

    private boolean isOverlap(DaySchedule a, DaySchedule b) {
        LocalTime aStart = LocalTime.parse(a.getStartTime());
        LocalTime aEnd = LocalTime.parse(a.getEndTime());
        LocalTime bStart = LocalTime.parse(b.getStartTime());
        LocalTime bEnd = LocalTime.parse(b.getEndTime());

        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private void validateSessionExists(String doctorId, LocalDate date, String sessionId) {

        WeeklySchedule schedule = weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("No schedule"));

        List<DaySchedule> slots = getSlotsByDay(schedule, date);

        boolean exists = slots != null &&
                slots.stream().anyMatch(s -> sessionId.equals(s.getSessionId()));

        if (!exists) {
            throw new ResourceNotFoundException("Session not found");
        }
    }

    private List<DaySchedule> getSlotsByDay(WeeklySchedule schedule, LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> schedule.getMonday();
            case TUESDAY -> schedule.getTuesday();
            case WEDNESDAY -> schedule.getWednesday();
            case THURSDAY -> schedule.getThursday();
            case FRIDAY -> schedule.getFriday();
            case SATURDAY -> schedule.getSaturday();
            case SUNDAY -> schedule.getSunday();
        };
    }

    private List<DaySchedule> assignSessionIds(List<DaySchedule> slots) {
        if (slots == null) return null;

        slots.forEach(s -> {
            if (s.getSessionId() == null || s.getSessionId().isBlank()) {
                s.setSessionId(UUID.randomUUID().toString());
            }
        });

        return slots;
    }

    // ───────────────── SECURITY ─────────────────

    private void validateOwnership(String doctorId, String authId) {

        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Unauthorized");
        }

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException("Doctor not active");
        }
    }
}