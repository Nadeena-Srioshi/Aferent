package com.aferent.appointment_service.service;

// service/SlotGenerationService.java
// This is the core of YOUR responsibility.
// Friend emits availability.updated → you consume it → generate dated slots.

import com.aferent.appointment_service.dto.DoctorScheduleDto;
import com.aferent.appointment_service.model.AppointmentType;
import com.aferent.appointment_service.model.GeneratedSlot;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotGenerationService {

    private final GeneratedSlotRepository slotRepository;

    @Value("${appointment.slot-generation-weeks-ahead}")
    private int weeksAhead;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    // Called when availability.updated Kafka event arrives
    public void regenerateSlotsForSchedule(DoctorScheduleDto schedule) {
        LocalDate today = LocalDate.now();

        // Delete all future unbooked slots for this schedule
        // (booked ones stay so existing appointments are not broken)
        slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                schedule.getId(), today);

        log.info("Regenerating slots for scheduleId={} doctor={}",
                schedule.getId(), schedule.getDoctorId());

        // Generate for the next N weeks
        List<GeneratedSlot> newSlots = new ArrayList<>();
        LocalDate cursor = today;
        LocalDate until  = today.plusWeeks(weeksAhead);

        while (!cursor.isAfter(until)) {
            String dayName = cursor.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();

            if (dayName.equals(schedule.getDayOfWeek().toUpperCase())) {
                if ("PHYSICAL".equals(schedule.getType())) {
                    newSlots.addAll(generatePhysicalSlots(schedule, cursor));
                } else {
                    newSlots.addAll(generateVideoSlots(schedule, cursor));
                }
            }
            cursor = cursor.plusDays(1);
        }

        slotRepository.saveAll(newSlots);
        log.info("Generated {} slots for scheduleId={}", newSlots.size(), schedule.getId());
    }

    private List<GeneratedSlot> generatePhysicalSlots(
            DoctorScheduleDto schedule, LocalDate date) {

        List<GeneratedSlot> slots  = new ArrayList<>();
        LocalTime start = LocalTime.parse(schedule.getStartTime(), TIME_FMT);
        LocalTime end   = LocalTime.parse(schedule.getEndTime(),   TIME_FMT);
        int duration    = schedule.getSessionDurationMinutes() != null
                ? schedule.getSessionDurationMinutes() : 15;

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
                    .hospitalName(schedule.getHospitalName())
                    .hospitalLocation(schedule.getHospitalAddress())
                    .consultationFee(schedule.getConsultationFee())
                    .booked(false)
                    .build());
            cursor = cursor.plusMinutes(duration);
            number++;
        }
        return slots;
    }

    private List<GeneratedSlot> generateVideoSlots(
            DoctorScheduleDto schedule, LocalDate date) {

        List<GeneratedSlot> slots  = new ArrayList<>();
        LocalTime start = LocalTime.parse(schedule.getStartTime(), TIME_FMT);
        LocalTime end   = LocalTime.parse(schedule.getEndTime(),   TIME_FMT);
        int duration    = schedule.getSessionDurationMinutes() != null
                ? schedule.getSessionDurationMinutes() : 60;

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
                    .consultationFee(schedule.getConsultationFee())
                    .booked(false)
                    .build());
            cursor = cursor.plusMinutes(duration);
            index++;
        }
        return slots;
    }
}