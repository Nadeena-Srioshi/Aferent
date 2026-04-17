package com.aferent.appointment_service.kafka;

import com.aferent.appointment_service.dto.DoctorScheduleDto;
import com.aferent.appointment_service.model.Appointment;
import com.aferent.appointment_service.repository.AppointmentRepository;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import com.aferent.appointment_service.service.SlotGenerationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final SlotGenerationService   slotGenerationService;
    private final GeneratedSlotRepository slotRepository;
    private final AppointmentRepository   appointmentRepository;
    private final ObjectMapper            objectMapper;
    private final WebClient.Builder       webClientBuilder;

    @Value("${services.doctor-service-url:http://doctor-service:3003}")
    private String doctorServiceUrl;

    /**
     * Doctor-service publishes one event per session when a weekly schedule is saved/updated.
     * We fetch the full schedule via HTTP and regenerate all slots.
     * Multiple events for the same doctorId are idempotent: each call deletes
     * existing unbooked slots and regenerates from the current schedule state.
     */
    @KafkaListener(topics = "doctor.schedule.weekly.upserted", groupId = "appointment-service")
    public void onWeeklyScheduleUpserted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String doctorId = node.path("doctorId").asText(null);
            if (doctorId == null || doctorId.isBlank()) {
                log.warn("doctor.schedule.weekly.upserted: missing doctorId in payload");
                return;
            }
            log.info("doctor.schedule.weekly.upserted received doctorId={}", doctorId);
            fetchAndRegenerateSlots(doctorId);
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.weekly.upserted: {}", e.getMessage(), e);
        }
    }

    /**
     * Doctor deleted their weekly schedule -- remove all future unbooked slots.
     */
    @KafkaListener(topics = "doctor.schedule.weekly.deleted", groupId = "appointment-service")
    public void onWeeklyScheduleDeleted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String doctorId = node.path("doctorId").asText(null);
            String scheduleId = node.path("id").asText(null);
            if (doctorId == null || doctorId.isBlank()) {
                log.warn("doctor.schedule.weekly.deleted: missing doctorId");
                return;
            }
            log.info("doctor.schedule.weekly.deleted doctorId={} scheduleId={}", doctorId, scheduleId);
            if (scheduleId != null && !scheduleId.isBlank()) {
                slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                        scheduleId, LocalDate.now());
                log.info("Deleted future unbooked slots for scheduleId={}", scheduleId);
            }
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.weekly.deleted: {}", e.getMessage(), e);
        }
    }

    /**
     * Doctor created/updated a schedule override (BLOCK, CANCEL_SESSION, or ADD extra session).
     *
     * BLOCK       → delete all unbooked slots for that doctor on that date.
     * CANCEL_SESSION → same — slots for the cancelled session are removed.
     * ADD         → generate extra one-off slots for that date using the override's
     *               time/type/hospital data; uses the slot's sessionId as the scheduleId
     *               so the slots can be cleaned up if the override is later deleted.
     */
    @KafkaListener(topics = "doctor.schedule.override.upserted", groupId = "appointment-service")
    public void onScheduleOverrideUpserted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String doctorId      = node.path("doctorId").asText(null);
            String overrideAction = node.path("overrideAction").asText(null);
            String dateStr        = node.path("date").asText(null);

            if (doctorId == null || dateStr == null || overrideAction == null) {
                log.warn("doctor.schedule.override.upserted: missing required fields in payload");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);

            switch (overrideAction) {
                case "BLOCK" -> {
                    slotRepository.deleteByDoctorIdAndDateAndBookedFalse(doctorId, date);
                    log.info("BLOCK override: deleted unbooked slots doctorId={} date={}", doctorId, date);
                }
                case "CANCEL_SESSION" -> {
                    slotRepository.deleteByDoctorIdAndDateAndBookedFalse(doctorId, date);
                    log.info("CANCEL_SESSION override: deleted unbooked slots doctorId={} date={} sessionId={}",
                            doctorId, date, node.path("sessionId").asText("-"));
                }
                case "ADD" -> {
                    String slotSessionId = node.path("id").asText(null);
                    String startTime     = node.path("startTime").asText(null);
                    String endTime       = node.path("endTime").asText(null);
                    String type          = node.path("type").asText(null);
                    String hospitalName  = node.path("hospitalName").asText(null);

                    if (slotSessionId == null || startTime == null || endTime == null || type == null) {
                        log.warn("OVERRIDE ADD: missing slot fields for doctorId={}", doctorId);
                        return;
                    }
                    slotGenerationService.generateSlotsForOverrideSlot(
                            slotSessionId, doctorId, date, startTime, endTime, type, hospitalName);
                }
                default -> log.warn("Unknown overrideAction={} for doctorId={}", overrideAction, doctorId);
            }
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.override.upserted: {}", e.getMessage(), e);
        }
    }

    /**
     * Doctor deleted a schedule override.
     *
     * ADD deleted  → remove the override slots (identified by the slot sessionId stored
     *                as scheduleId on GeneratedSlot).
     * BLOCK/CANCEL_SESSION deleted → the day is unblocked; fetch the weekly schedule from
     *                doctor-service and regenerate slots for that specific date.
     */
    @KafkaListener(topics = "doctor.schedule.override.deleted", groupId = "appointment-service")
    public void onScheduleOverrideDeleted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String doctorId     = node.path("doctorId").asText(null);
            String deletedAction = node.path("deletedAction").asText(null);
            String dateStr       = node.path("date").asText(null);

            if (doctorId == null || dateStr == null || deletedAction == null) {
                log.warn("doctor.schedule.override.deleted: missing required fields in payload");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);

            switch (deletedAction) {
                case "ADD" -> {
                    String sessionId = node.path("id").asText(null);
                    if (sessionId != null) {
                        slotRepository.deleteByScheduleIdAndDateAndBookedFalse(sessionId, date);
                        log.info("OVERRIDE_DELETE ADD: removed override slots scheduleId={} date={}", sessionId, date);
                    }
                }
                case "BLOCK", "CANCEL_SESSION" -> {
                    fetchAndRegenerateSlotsForDate(doctorId, date);
                    log.info("OVERRIDE_DELETE {}: restored weekly slots for doctorId={} date={}", deletedAction, doctorId, date);
                }
                default -> log.warn("Unknown deletedAction={} for doctorId={}", deletedAction, doctorId);
            }
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.override.deleted: {}", e.getMessage(), e);
        }
    }

    private void fetchAndRegenerateSlotsForDate(String doctorId, LocalDate date) {
        try {
            WebClient client = webClientBuilder.baseUrl(doctorServiceUrl).build();
            DoctorScheduleDto schedule = client.get()
                    .uri("/doctors/{doctorId}/schedule/weekly", doctorId)
                    .retrieve()
                    .bodyToMono(DoctorScheduleDto.class)
                    .block();
            if (schedule == null) {
                log.warn("No schedule from doctor-service for doctorId={}", doctorId);
                return;
            }
            slotGenerationService.generateSlotsForScheduleOnDate(schedule, date, true);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("doctor-service 404 for doctorId={} — no weekly schedule to restore", doctorId);
        } catch (Exception e) {
            log.error("Failed to restore slots for doctorId={} date={}: {}", doctorId, date, e.getMessage(), e);
        }
    }

    /**
     * Telemedicine-service publishes this when the first participant joins a video call.
     * We save the session link on the appointment so patients/doctors can find it later.
     */
    @KafkaListener(topics = "telemedicine.session.started", groupId = "appointment-service")
    public void onTelemedicineSessionStarted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String appointmentId = node.path("appointmentId").asText(null);
            String sessionLink = node.path("sessionLink").asText(null);
            if (appointmentId == null || appointmentId.isBlank()) {
                log.warn("telemedicine.session.started: missing appointmentId");
                return;
            }
            if (sessionLink == null || sessionLink.isBlank()) {
                log.warn("telemedicine.session.started: missing sessionLink for appointmentId={}", appointmentId);
                return;
            }
            appointmentRepository.findById(appointmentId).ifPresentOrElse(
                    appointment -> {
                        appointment.setVideoSessionLink(sessionLink);
                        appointmentRepository.save(appointment);
                        log.info("Saved videoSessionLink for appointmentId={}", appointmentId);
                    },
                    () -> log.warn("telemedicine.session.started: appointment {} not found", appointmentId)
            );
        } catch (Exception e) {
            log.error("Error processing telemedicine.session.started: {}", e.getMessage(), e);
        }
    }

    private void fetchAndRegenerateSlots(String doctorId) {
        try {
            WebClient client = webClientBuilder.baseUrl(doctorServiceUrl).build();
            DoctorScheduleDto schedule = client.get()
                    .uri("/doctors/{doctorId}/schedule/weekly", doctorId)
                    .retrieve()
                    .bodyToMono(DoctorScheduleDto.class)
                    .block();

            if (schedule == null) {
                log.warn("No schedule returned from doctor-service for doctorId={}", doctorId);
                return;
            }
            slotGenerationService.regenerateSlotsForSchedule(schedule);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("doctor-service returned 404 for doctorId={} - schedule may have been deleted", doctorId);
        } catch (Exception e) {
            log.error("Failed to fetch/regenerate slots for doctorId={}: {}", doctorId, e.getMessage(), e);
        }
    }
}