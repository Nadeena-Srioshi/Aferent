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

    @Value("${appointment.slot-generation-weeks-ahead:4}")
    private int weeksAhead;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Called when doctor.schedule.weekly.upserted Kafka event arrives
     * This handles the weekly schedule structure with multiple sessions per day
     */
    public void regenerateSlotsForSchedule(DoctorScheduleDto schedule) {
        LocalDate today = LocalDate.now();

        // Delete all future unbooked slots for this schedule
        slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                schedule.getId(), today);

        log.info("Regenerating slots for scheduleId={} doctor={}",
                schedule.getId(), schedule.getDoctorId());

        // Generate for the next N weeks
        List<GeneratedSlot> newSlots = new ArrayList<>();
        LocalDate cursor = today;
        LocalDate until = today.plusWeeks(weeksAhead);

        while (!cursor.isAfter(until)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            
            // Get sessions for this day
            List<DoctorScheduleDto.SessionDto> daySessions = getSessionsForDay(schedule, dayOfWeek);
            
            if (daySessions != null && !daySessions.isEmpty()) {
                for (DoctorScheduleDto.SessionDto session : daySessions) {
                    if ("IN_PERSON".equalsIgnoreCase(session.getType()) || 
                        "PHYSICAL".equalsIgnoreCase(session.getType())) {
                        newSlots.addAll(generatePhysicalSlots(schedule, session, cursor));
                    } else if ("VIDEO".equalsIgnoreCase(session.getType())) {
                        newSlots.addAll(generateVideoSlots(schedule, session, cursor));
                    }
                }
            }
            
            cursor = cursor.plusDays(1);
        }

        if (!newSlots.isEmpty()) {
            slotRepository.saveAll(newSlots);
            log.info("Generated {} slots for scheduleId={}", newSlots.size(), schedule.getId());
        } else {
            log.warn("No slots generated for scheduleId={}", schedule.getId());
        }
    }

    /**
     * Generate slots for a specific date (used for overrides)
     */
    public void generateSlotsForScheduleOnDate(DoctorScheduleDto schedule,
                                               LocalDate date,
                                               boolean replaceUnbookedForDate) {
        if (replaceUnbookedForDate) {
            slotRepository.deleteByScheduleIdAndDateAndBookedFalse(schedule.getId(), date);
        }

        List<GeneratedSlot> slots = new ArrayList<>();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorScheduleDto.SessionDto> daySessions = getSessionsForDay(schedule, dayOfWeek);
        
        if (daySessions != null && !daySessions.isEmpty()) {
            for (DoctorScheduleDto.SessionDto session : daySessions) {
                if ("IN_PERSON".equalsIgnoreCase(session.getType()) || 
                    "PHYSICAL".equalsIgnoreCase(session.getType())) {
                    slots.addAll(generatePhysicalSlots(schedule, session, date));
                } else if ("VIDEO".equalsIgnoreCase(session.getType())) {
                    slots.addAll(generateVideoSlots(schedule, session, date));
                }
            }
        }

        if (!slots.isEmpty()) {
            slotRepository.saveAll(slots);
            log.info("Generated {} slots for scheduleId={} on {}",
                    slots.size(), schedule.getId(), date);
        }
    }

    /**
     * Get sessions for a specific day of the week from the schedule
     */
    private List<DoctorScheduleDto.SessionDto> getSessionsForDay(
            DoctorScheduleDto schedule, DayOfWeek dayOfWeek) {
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

    /**
     * Generate physical (in-person) appointment slots
     */
    private List<GeneratedSlot> generatePhysicalSlots(
            DoctorScheduleDto schedule,
            DoctorScheduleDto.SessionDto session,
            LocalDate date) {

        List<GeneratedSlot> slots = new ArrayList<>();
        
        try {
            LocalTime start = LocalTime.parse(session.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(session.getEndTime(), TIME_FMT);
            
            // Default duration 15 minutes for physical appointments
            int duration = session.getSessionDurationMinutes() != null
                    ? session.getSessionDurationMinutes() : 15;

            LocalTime cursor = start;
            int number = 1;

            while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
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
                        .consultationFee(session.getConsultationFee() != null 
                                ? session.getConsultationFee() : 0.0)
                        .booked(false)
                        .build());
                cursor = cursor.plusMinutes(duration);
                number++;
            }
            
            log.debug("Generated {} physical slots for session {} on {}", 
                    slots.size(), session.getSessionId(), date);
        } catch (Exception e) {
            log.error("Error generating physical slots for session {} on {}: {}", 
                    session.getSessionId(), date, e.getMessage());
        }
        
        return slots;
    }

    /**
     * Generate video appointment slots
     */
    private List<GeneratedSlot> generateVideoSlots(
            DoctorScheduleDto schedule,
            DoctorScheduleDto.SessionDto session,
            LocalDate date) {

        List<GeneratedSlot> slots = new ArrayList<>();
        
        try {
            LocalTime start = LocalTime.parse(session.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(session.getEndTime(), TIME_FMT);
            
            // Default duration 30 minutes for video appointments
            int duration = session.getSessionDurationMinutes() != null
                    ? session.getSessionDurationMinutes() : 30;

            LocalTime cursor = start;
            int index = 1;

            while (cursor.plusMinutes(duration).compareTo(end) <= 0) {
                slots.add(GeneratedSlot.builder()
                        .scheduleId(schedule.getId())
                        .doctorId(schedule.getDoctorId())
                        .type(AppointmentType.VIDEO)
                        .date(date)
                        .startTime(cursor)
                        .endTime(cursor.plusMinutes(duration))
                        .videoSlotId(String.format("slot_%03d", index))
                        .consultationFee(session.getConsultationFee() != null 
                                ? session.getConsultationFee() : 0.0)
                        .booked(false)
                        .build());
                cursor = cursor.plusMinutes(duration);
                index++;
            }
            
            log.debug("Generated {} video slots for session {} on {}", 
                    slots.size(), session.getSessionId(), date);
        } catch (Exception e) {
            log.error("Error generating video slots for session {} on {}: {}", 
                    session.getSessionId(), date, e.getMessage());
        }
        
        return slots;
    }
}