package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.ScheduleOverrideRequest;
import com.aferent.doctor_service.dto.WeeklyScheduleRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.ScheduleOverride;
import com.aferent.doctor_service.model.WeeklySchedule;
import com.aferent.doctor_service.model.WeeklySchedule.DaySchedule;
import com.aferent.doctor_service.kafka.DoctorEventProducer;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.HospitalRepository;
import com.aferent.doctor_service.repository.ScheduleOverrideRepository;
import com.aferent.doctor_service.repository.WeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorScheduleService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorEventProducer doctorEventProducer;

    // weekly schedule   ********

    public WeeklySchedule setWeeklySchedule(String doctorId, String authId,
                                            WeeklyScheduleRequest request) {

        Doctor doctor = validateOwnership(doctorId, authId);

        validateAllDays(request, doctor);

        WeeklySchedule schedule = weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElse(WeeklySchedule.builder().doctorId(doctorId).build());

        schedule.setMonday(assignSessionIds(request.getMonday()));
        schedule.setTuesday(assignSessionIds(request.getTuesday()));
        schedule.setWednesday(assignSessionIds(request.getWednesday()));
        schedule.setThursday(assignSessionIds(request.getThursday()));
        schedule.setFriday(assignSessionIds(request.getFriday()));
        schedule.setSaturday(assignSessionIds(request.getSaturday()));
        schedule.setSunday(assignSessionIds(request.getSunday()));

        WeeklySchedule saved = weeklyScheduleRepository.save(schedule);
        publishWeeklyUpsertEvents(saved);
        log.info("Weekly schedule saved for doctorId={}", doctorId);

        return saved;
    }

    public void deleteWeeklySchedule(String doctorId, String authId) {
        validateOwnership(doctorId, authId);

        WeeklySchedule schedule = weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("No schedule found"));

        weeklyScheduleRepository.deleteByDoctorId(doctorId);
        publishWeeklyDeleteEvents(schedule);
        log.info("Weekly schedule deleted for doctorId={}", doctorId);
    }

    public WeeklySchedule getWeeklySchedule(String doctorId) {
        return weeklyScheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("No schedule found"));
    }

    // overrides    ********

    public ScheduleOverride addOverride(String doctorId, String authId,
                                        ScheduleOverrideRequest request) {

        Doctor doctor = validateOwnership(doctorId, authId);

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot modify past dates");
        }

        switch (request.getAction()) {

            case ADD -> {
                if (request.getSlots() == null || request.getSlots().isEmpty()) {
                    throw new IllegalArgumentException("Slots required for ADD");
                }
                validateSlotList(request.getSlots(), doctor);
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

        ScheduleOverride saved = scheduleOverrideRepository.save(override);
        publishOverrideUpsertEvents(saved);
        log.info("Override saved doctorId={} date={} action={}",
                doctorId, request.getDate(), request.getAction());

        return saved;
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
        publishOverrideDeletedEvent(override);
        log.info("Override deleted overrideId={} doctorId={}", overrideId, doctorId);
    }

    // validations    *********

    private void validateAllDays(WeeklyScheduleRequest request, Doctor doctor) {
        validateSlotList(request.getMonday(), doctor);
        validateSlotList(request.getTuesday(), doctor);
        validateSlotList(request.getWednesday(), doctor);
        validateSlotList(request.getThursday(), doctor);
        validateSlotList(request.getFriday(), doctor);
        validateSlotList(request.getSaturday(), doctor);
        validateSlotList(request.getSunday(), doctor);
    }

    private void validateSlotList(List<DaySchedule> slots, Doctor doctor) {
        if (slots == null || slots.isEmpty()) return;

        for (DaySchedule slot : slots) {
            validateSingleSlot(slot, doctor);
        }

        // overlap check
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (isOverlap(slots.get(i), slots.get(j))) {
                    throw new IllegalArgumentException(
                            "Time slots overlap: "
                                    + slots.get(i).getStartTime() + "-" + slots.get(i).getEndTime()
                                    + " overlaps with "
                                    + slots.get(j).getStartTime() + "-" + slots.get(j).getEndTime());
                }
            }
        }
    }

    private void validateSingleSlot(DaySchedule slot, Doctor doctor) {

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
            throw new IllegalArgumentException(
                    "startTime must be before endTime: "
                            + slot.getStartTime() + " is not before " + slot.getEndTime());
        }

        if (slot.getType() == null) {
            throw new IllegalArgumentException("Session type required");
        }

        // IN_PERSON validation
        if (slot.getType() == DaySchedule.SessionType.IN_PERSON) {

            if (slot.getHospital() == null || slot.getHospital().isBlank()) {
                throw new IllegalArgumentException("Hospital required for IN_PERSON");
            }

            if (!hospitalRepository.existsById(slot.getHospital())) {
                throw new IllegalArgumentException("Invalid hospitalId: " + slot.getHospital());
            }

            if (doctor.getHospitals() == null ||
                    !doctor.getHospitals().contains(slot.getHospital())) {
                throw new IllegalArgumentException(
                        "Hospital " + slot.getHospital() + " is not assigned to this doctor");
            }
        }

        // VIDEO validation
        if (slot.getType() == DaySchedule.SessionType.VIDEO) {
            slot.setHospital(null); // auto-clean
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

        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("No sessions for " + date.getDayOfWeek());
        }

        boolean exists = slots.stream()
                .anyMatch(s -> sessionId.equals(s.getSessionId()));

        if (!exists) {
            throw new ResourceNotFoundException(
                    "Session not found for " + date.getDayOfWeek());
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

    private void publishWeeklyUpsertEvents(WeeklySchedule schedule) {
        publishWeeklyDayUpsert(schedule.getDoctorId(), "MONDAY", schedule.getMonday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "TUESDAY", schedule.getTuesday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "WEDNESDAY", schedule.getWednesday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "THURSDAY", schedule.getThursday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "FRIDAY", schedule.getFriday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "SATURDAY", schedule.getSaturday());
        publishWeeklyDayUpsert(schedule.getDoctorId(), "SUNDAY", schedule.getSunday());
    }

    private void publishWeeklyDeleteEvents(WeeklySchedule schedule) {
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getMonday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getTuesday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getWednesday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getThursday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getFriday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getSaturday());
        publishWeeklyDayDelete(schedule.getDoctorId(), schedule.getSunday());
    }

    private void publishWeeklyDayUpsert(String doctorId, String dayOfWeek, List<DaySchedule> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }

        slots.forEach(slot -> doctorEventProducer.publishWeeklyUpserted(
                doctorId,
                slot.getSessionId(),
                dayOfWeek,
                slot.getStartTime(),
                slot.getEndTime(),
                mapType(slot.getType()),
                slot.getHospital()
        ));
    }

    private void publishWeeklyDayDelete(String doctorId, List<DaySchedule> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }

        slots.forEach(slot -> doctorEventProducer.publishWeeklyDeleted(doctorId, slot.getSessionId()));
    }

    private void publishOverrideUpsertEvents(ScheduleOverride override) {
        switch (override.getAction()) {
            case BLOCK -> doctorEventProducer.publishOverrideUpsertedBlock(
                    override.getDoctorId(),
                    override.getDate()
            );
            case ADD -> {
                if (override.getSlots() == null || override.getSlots().isEmpty()) {
                    return;
                }

                override.getSlots().forEach(slot ->
                        doctorEventProducer.publishOverrideUpsertedAdd(
                                override.getDoctorId(),
                                buildOverrideScheduleId(override.getId(), slot.getSessionId()),
                                override.getDate(),
                                slot.getStartTime(),
                                slot.getEndTime(),
                                mapType(slot.getType()),
                                slot.getHospital()
                        )
                );
            }
            case CANCEL_SESSION -> doctorEventProducer.publishOverrideUpsertedCancelSession(
                    override.getDoctorId(),
                    override.getDate(),
                    override.getSessionId()
            );
        }
    }

    private void publishOverrideDeletedEvent(ScheduleOverride override) {
        switch (override.getAction()) {
            case ADD -> {
                if (override.getSlots() == null || override.getSlots().isEmpty()) {
                    return;
                }
                override.getSlots().forEach(slot ->
                        doctorEventProducer.publishOverrideDeleted(
                                override.getDoctorId(),
                                "ADD",
                                override.getDate(),
                                buildOverrideScheduleId(override.getId(), slot.getSessionId()),
                                null,
                                null
                        )
                );
            }
            case BLOCK -> doctorEventProducer.publishOverrideDeleted(
                    override.getDoctorId(),
                    "BLOCK",
                    override.getDate(),
                    null,
                    null,
                    buildRestoreSchedulesForDate(override.getDoctorId(), override.getDate(), null)
            );
            case CANCEL_SESSION -> doctorEventProducer.publishOverrideDeleted(
                    override.getDoctorId(),
                    "CANCEL_SESSION",
                    override.getDate(),
                    null,
                    override.getSessionId(),
                    buildRestoreSchedulesForDate(override.getDoctorId(), override.getDate(), override.getSessionId())
            );
        }
    }

    private List<Map<String, Object>> buildRestoreSchedulesForDate(String doctorId,
                                                                   LocalDate date,
                                                                   String onlySessionId) {
        WeeklySchedule schedule = weeklyScheduleRepository.findByDoctorId(doctorId).orElse(null);
        if (schedule == null) {
            return List.of();
        }

        List<DaySchedule> daySchedules = getSlotsByDay(schedule, date);
        if (daySchedules == null || daySchedules.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> restoreSchedules = new ArrayList<>();
        for (DaySchedule slot : daySchedules) {
            if (onlySessionId != null && !onlySessionId.equals(slot.getSessionId())) {
                continue;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", slot.getSessionId());
            data.put("doctorId", doctorId);
            data.put("dayOfWeek", date.getDayOfWeek().name());
            data.put("startTime", slot.getStartTime());
            data.put("endTime", slot.getEndTime());
            data.put("type", mapType(slot.getType()));
            data.put("hospitalName", slot.getHospital());
            data.put("active", true);
            restoreSchedules.add(data);
        }

        return restoreSchedules;
    }

    private String buildOverrideScheduleId(String overrideId, String sessionId) {
        return "override-" + overrideId + "-" + sessionId;
    }

    private String mapType(DaySchedule.SessionType type) {
        if (type == DaySchedule.SessionType.IN_PERSON) {
            return "PHYSICAL";
        }
        return "VIDEO";
    }

    // security    *******

    private Doctor validateOwnership(String doctorId, String authId) {

        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (!doctor.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException(
                    "You can only manage your own schedule");
        }

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException(
                    "Account not active. Current status: " + doctor.getStatus());
        }

        return doctor;
    }
}