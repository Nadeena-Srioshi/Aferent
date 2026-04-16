package com.aferent.appointment_service.kafka;

import com.aferent.appointment_service.dto.DoctorScheduleDto;
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
