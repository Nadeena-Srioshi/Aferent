package com.aferent.appointment_service.service;

import com.aferent.appointment_service.dto.DoctorScheduleDto;
import com.aferent.appointment_service.model.AppointmentType;
import com.aferent.appointment_service.model.GeneratedSlot;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotGenerationService {

    private final GeneratedSlotRepository slotRepository;

    private static final int SAVE_BATCH_SIZE = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${appointment.slot-generation-weeks-ahead:2}")
    private int weeksAhead;

    @Value("${appointment.max-slots-per-session:64}")
    private int maxSlotsPerSession;

    @Value("${appointment.max-slots-per-regeneration:600}")
    private int maxSlotsPerRegeneration;

    @Value("${appointment.max-slots-per-date:120}")
    private int maxSlotsPerDate;

    /**
     * Called when doctor.schedule.weekly.upserted Kafka event arrives.
     */
    public void regenerateSlotsForSchedule(DoctorScheduleDto schedule) {
        LocalDate today = LocalDate.now();

        slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                schedule.getId(), today);

        log.info("Regenerating slots for scheduleId={} doctor={}",
                schedule.getId(), schedule.getDoctorId());

        List<GeneratedSlot> buffer = new ArrayList<>(SAVE_BATCH_SIZE);
        LocalDate cursor = today;
        LocalDate until = today.plusWeeks(weeksAhead);
        int generatedCount = 0;

        while (!cursor.isAfter(until)) {
            if (generatedCount >= maxSlotsPerRegeneration) {
                log.warn("Reached max slot cap ({}) for scheduleId={}; stopping generation window",
                        maxSlotsPerRegeneration, schedule.getId());
                break;
            }

            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            List<DoctorScheduleDto.SessionDto> daySessions = getSessionsForDay(schedule, dayOfWeek);

            if (daySessions != null && !daySessions.isEmpty()) {
                for (DoctorScheduleDto.SessionDto session : daySessions) {
                    int remaining = maxSlotsPerRegeneration - generatedCount;
                    if (remaining <= 0) {
                        break;
                    }

                    List<GeneratedSlot> generated = null;
                    if ("IN_PERSON".equalsIgnoreCase(session.getType())
                            || "PHYSICAL".equalsIgnoreCase(session.getType())) {
                        generated = generatePhysicalSlots(schedule, session, cursor);
                    } else if ("VIDEO".equalsIgnoreCase(session.getType())) {
                        generated = generateVideoSlots(schedule, session, cursor);
                    }

                    generatedCount += appendAndFlush(buffer, generated, remaining);
                }
            }

            cursor = cursor.plusDays(1);
        }

        if (!buffer.isEmpty()) {
            slotRepository.saveAll(buffer);
            generatedCount += buffer.size();
            buffer.clear();
        }

        if (generatedCount > 0) {
            log.info("Generated {} slots for scheduleId={}", generatedCount, schedule.getId());
        } else {
            log.warn("No slots generated for scheduleId={}", schedule.getId());
        }
    }

    /**
     * Generate slots for a specific date (used for overrides restore).
     */
    public void generateSlotsForScheduleOnDate(DoctorScheduleDto schedule,
                                               LocalDate date,
                                               boolean replaceUnbookedForDate) {
        if (replaceUnbookedForDate) {
            slotRepository.deleteByScheduleIdAndDateAndBookedFalse(schedule.getId(), date);
        }

        List<GeneratedSlot> slots = new ArrayList<>();
        int generatedCount = 0;

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorScheduleDto.SessionDto> daySessions = getSessionsForDay(schedule, dayOfWeek);

        if (daySessions != null && !daySessions.isEmpty()) {
            for (DoctorScheduleDto.SessionDto session : daySessions) {
                int remaining = maxSlotsPerDate - generatedCount;
                if (remaining <= 0) {
                    log.warn("Reached max daily slot cap ({}) for scheduleId={} on {}",
                            maxSlotsPerDate, schedule.getId(), date);
                    break;
                }

                List<GeneratedSlot> generated = null;
                if ("IN_PERSON".equalsIgnoreCase(session.getType())
                        || "PHYSICAL".equalsIgnoreCase(session.getType())) {
                    generated = generatePhysicalSlots(schedule, session, date);
                } else if ("VIDEO".equalsIgnoreCase(session.getType())) {
                    generated = generateVideoSlots(schedule, session, date);
                }

                if (generated == null || generated.isEmpty()) {
                    continue;
                }

                int toTake = Math.min(remaining, generated.size());
                slots.addAll(generated.subList(0, toTake));
                generatedCount += toTake;
            }
        }

        if (!slots.isEmpty()) {
            slotRepository.saveAll(slots);
            log.info("Generated {} slots for scheduleId={} on {}", slots.size(), schedule.getId(), date);
        }
    }

    /**
     * Generates slots for a single override ADD session.
     */
    public void generateSlotsForOverrideSlot(String sessionId,
                                             String doctorId,
                                             LocalDate date,
                                             String startTime,
                                             String endTime,
                                             String type,
                                             String hospitalName,
                                             Double consultationFee) {
        List<GeneratedSlot> slots = new ArrayList<>();
        try {
            LocalTime start = LocalTime.parse(startTime, TIME_FMT);
            LocalTime end = LocalTime.parse(endTime, TIME_FMT);

            if (!start.isBefore(end)) {
                log.warn("Invalid override time range for doctorId={} on {}: {}-{}", doctorId, date, startTime, endTime);
                return;
            }

            if ("IN_PERSON".equalsIgnoreCase(type) || "PHYSICAL".equalsIgnoreCase(type)) {
                int duration = 15;
                LocalTime cursor = start;
                int number = 1;
                int generatedForSession = 0;
                while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
                    if (generatedForSession >= maxSlotsPerSession) {
                        log.warn("Override slot generation capped at {} slots for doctorId={} on {} sessionId={}",
                                maxSlotsPerSession, doctorId, date, sessionId);
                        break;
                    }
                    slots.add(GeneratedSlot.builder()
                            .scheduleId(sessionId)
                            .doctorId(doctorId)
                            .type(AppointmentType.PHYSICAL)
                            .date(date)
                            .startTime(cursor)
                            .endTime(cursor.plusMinutes(duration))
                            .appointmentNumber(number)
                            .hospitalName(hospitalName)
                            .consultationFee(consultationFee != null ? consultationFee : 0.0)
                            .booked(false)
                            .build());
                    cursor = cursor.plusMinutes(duration);
                    number++;
                    generatedForSession++;
                }
            } else if ("VIDEO".equalsIgnoreCase(type)) {
                int duration = 30;
                LocalTime cursor = start;
                int index = 1;
                int generatedForSession = 0;
                while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
                    if (generatedForSession >= maxSlotsPerSession) {
                        log.warn("Override slot generation capped at {} slots for doctorId={} on {} sessionId={}",
                                maxSlotsPerSession, doctorId, date, sessionId);
                        break;
                    }
                    slots.add(GeneratedSlot.builder()
                            .scheduleId(sessionId)
                            .doctorId(doctorId)
                            .type(AppointmentType.VIDEO)
                            .date(date)
                            .startTime(cursor)
                            .endTime(cursor.plusMinutes(duration))
                            .videoSlotId(String.format("slot_%03d", index))
                            .consultationFee(consultationFee != null ? consultationFee : 0.0)
                            .booked(false)
                            .build());
                    cursor = cursor.plusMinutes(duration);
                    index++;
                    generatedForSession++;
                }
            }

            if (!slots.isEmpty()) {
                slotRepository.saveAll(slots);
                log.info("Generated {} override ADD slots for doctorId={} on {} sessionId={}",
                        slots.size(), doctorId, date, sessionId);
            } else {
                log.warn("No override ADD slots generated for doctorId={} on {} (empty time range?)", doctorId, date);
            }
        } catch (Exception e) {
            log.error("Error generating override ADD slots for doctorId={} on {}: {}", doctorId, date, e.getMessage(), e);
        }
    }

    private int appendAndFlush(List<GeneratedSlot> buffer,
                               List<GeneratedSlot> generated,
                               int remainingCapacity) {
        if (generated == null || generated.isEmpty() || remainingCapacity <= 0) {
            return 0;
        }

        int totalAdded = Math.min(remainingCapacity, generated.size());
        buffer.addAll(generated.subList(0, totalAdded));

        if (buffer.size() >= SAVE_BATCH_SIZE) {
            slotRepository.saveAll(buffer);
            buffer.clear();
        }

        return totalAdded;
    }

    private List<DoctorScheduleDto.SessionDto> getSessionsForDay(DoctorScheduleDto schedule, DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:    return schedule.getMonday();
            case TUESDAY:   return schedule.getTuesday();
            case WEDNESDAY: return schedule.getWednesday();
            case THURSDAY:  return schedule.getThursday();
            case FRIDAY:    return schedule.getFriday();
            case SATURDAY:  return schedule.getSaturday();
            case SUNDAY:    return schedule.getSunday();
            default:        return null;
        }
    }

    private List<GeneratedSlot> generatePhysicalSlots(DoctorScheduleDto schedule,
                                                      DoctorScheduleDto.SessionDto session,
                                                      LocalDate date) {
        List<GeneratedSlot> slots = new ArrayList<>();

        try {
            LocalTime start = LocalTime.parse(session.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(session.getEndTime(), TIME_FMT);

            if (!start.isBefore(end)) {
                log.warn("Skipping invalid physical session {} on {}: {}-{}",
                        session.getSessionId(), date, session.getStartTime(), session.getEndTime());
                return slots;
            }

            int duration = resolveDurationMinutes(session.getSessionDurationMinutes(), 15,
                    "PHYSICAL", session.getSessionId(), date);

            LocalTime cursor = start;
            int number = 1;
            int generatedForSession = 0;

            while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
                if (generatedForSession >= maxSlotsPerSession) {
                    log.warn("Physical session {} on {} exceeded cap of {} slots; stopping generation",
                            session.getSessionId(), date, maxSlotsPerSession);
                    break;
                }

                slots.add(GeneratedSlot.builder()
                        .scheduleId(schedule.getId())
                        .doctorId(schedule.getDoctorId())
                        .type(AppointmentType.PHYSICAL)
                        .date(date)
                        .startTime(cursor)
                        .endTime(cursor.plusMinutes(duration))
                        .appointmentNumber(number)
                        .hospitalName(session.getHospitalName())
                        .hospitalLocation(session.getHospitalAddress())
                        .consultationFee(session.getConsultationFee() != null ? session.getConsultationFee() : 0.0)
                        .booked(false)
                        .build());

                cursor = cursor.plusMinutes(duration);
                number++;
                generatedForSession++;
            }

            log.debug("Generated {} physical slots for session {} on {}",
                    slots.size(), session.getSessionId(), date);
        } catch (Exception e) {
            log.error("Error generating physical slots for session {} on {}: {}",
                    session.getSessionId(), date, e.getMessage());
        }

        return slots;
    }

    private List<GeneratedSlot> generateVideoSlots(DoctorScheduleDto schedule,
                                                   DoctorScheduleDto.SessionDto session,
                                                   LocalDate date) {
        List<GeneratedSlot> slots = new ArrayList<>();

        try {
            LocalTime start = LocalTime.parse(session.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(session.getEndTime(), TIME_FMT);

            if (!start.isBefore(end)) {
                log.warn("Skipping invalid video session {} on {}: {}-{}",
                        session.getSessionId(), date, session.getStartTime(), session.getEndTime());
                return slots;
            }

            int duration = resolveDurationMinutes(session.getSessionDurationMinutes(), 30,
                    "VIDEO", session.getSessionId(), date);

            LocalTime cursor = start;
            int index = 1;
            int generatedForSession = 0;

            while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
                if (generatedForSession >= maxSlotsPerSession) {
                    log.warn("Video session {} on {} exceeded cap of {} slots; stopping generation",
                            session.getSessionId(), date, maxSlotsPerSession);
                    break;
                }

                slots.add(GeneratedSlot.builder()
                        .scheduleId(schedule.getId())
                        .doctorId(schedule.getDoctorId())
                        .type(AppointmentType.VIDEO)
                        .date(date)
                        .startTime(cursor)
                        .endTime(cursor.plusMinutes(duration))
                        .videoSlotId(String.format("slot_%03d", index))
                        .consultationFee(session.getConsultationFee() != null ? session.getConsultationFee() : 0.0)
                        .booked(false)
                        .build());

                cursor = cursor.plusMinutes(duration);
                index++;
                generatedForSession++;
            }

            log.debug("Generated {} video slots for session {} on {}",
                    slots.size(), session.getSessionId(), date);
        } catch (Exception e) {
            log.error("Error generating video slots for session {} on {}: {}",
                    session.getSessionId(), date, e.getMessage());
        }

        return slots;
    }

    private int resolveDurationMinutes(Integer rawDuration,
                                       int defaultDuration,
                                       String type,
                                       String sessionId,
                                       LocalDate date) {
        if (rawDuration == null) {
            return defaultDuration;
        }

        if (rawDuration <= 0) {
            log.warn("Invalid {} session duration={} for session {} on {}; using default {}",
                    type, rawDuration, sessionId, date, defaultDuration);
            return defaultDuration;
        }

        return rawDuration;
    }
}
