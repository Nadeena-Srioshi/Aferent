package com.aferent.appointment_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aferent.appointment_service.dto.DoctorScheduleDto;
import com.aferent.appointment_service.model.Appointment;
import com.aferent.appointment_service.model.AppointmentStatus;
import com.aferent.appointment_service.repository.AppointmentRepository;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import com.aferent.appointment_service.service.SlotGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final AppointmentRepository   appointmentRepository;
    private final GeneratedSlotRepository slotRepository;
    private final SlotGenerationService   slotGenerationService;
    private final KafkaProducer           kafkaProducer;
    private final ObjectMapper            objectMapper;

    // Friend's Doctor Service emits this when schedule is created/updated/deleted
    // You consume it and regenerate slots
    @KafkaListener(topics = "availability.updated", groupId = "appointment-service")
    public void onAvailabilityUpdated(String message) {
        try {
            DoctorScheduleDto schedule = objectMapper.readValue(
                    message, DoctorScheduleDto.class);
            log.info("availability.updated received scheduleId={}", schedule.getId());

            if (!schedule.isActive()) {
                // Doctor deleted this schedule — remove all future unbooked slots
                slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                        schedule.getId(), java.time.LocalDate.now());
                log.info("Slots deleted for inactive scheduleId={}", schedule.getId());
            } else {
                slotGenerationService.regenerateSlotsForSchedule(schedule);
            }
        } catch (Exception e) {
            log.error("Error processing availability.updated: {}", e.getMessage());
        }
    }

    // Payment service confirms payment succeeded → set appointment CONFIRMED
    @KafkaListener(topics = "payment.success", groupId = "appointment-service")
    public void onPaymentSuccess(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String appointmentId = node.get("appointmentId").asText();
            String paymentId     = node.get("paymentId").asText();

            Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
            if (opt.isPresent()) {
                Appointment a = opt.get();
                a.setStatus(AppointmentStatus.CONFIRMED);
                a.setPaymentId(paymentId);
                a.setConfirmedAt(LocalDateTime.now());
                a.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(a);

                // Tell Doctor Service to evict Redis slot
                kafkaProducer.publishSlotBooked(a);

                // Tell Notification Service to send confirmation SMS/email
                kafkaProducer.publishAppointmentConfirmed(a);

                log.info("Appointment {} CONFIRMED after payment success", appointmentId);
            }
        } catch (Exception e) {
            log.error("Error processing payment.success: {}", e.getMessage());
        }
    }

    // Payment failed → mark appointment, release slot back
    @KafkaListener(topics = "payment.failed", groupId = "appointment-service")
    public void onPaymentFailed(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String appointmentId = node.get("appointmentId").asText();

            Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
            if (opt.isPresent()) {
                Appointment a = opt.get();
                a.setStatus(AppointmentStatus.PAYMENT_FAILED);
                a.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(a);

                // Release slot so next patient can take it
                if (a.getGeneratedSlotId() != null) {
                    slotRepository.findById(a.getGeneratedSlotId()).ifPresent(slot -> {
                        slot.setBooked(false);
                        slotRepository.save(slot);
                    });
                }
                log.info("Appointment {} PAYMENT_FAILED — slot released", appointmentId);
            }
        } catch (Exception e) {
            log.error("Error processing payment.failed: {}", e.getMessage());
        }
    }

    // Telemedicine service generates video link — save it to appointment
    @KafkaListener(topics = "telemedicine.session.started", groupId = "appointment-service")
    public void onTelemedicineSession(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String appointmentId = node.get("appointmentId").asText();
            String sessionLink   = node.get("sessionLink").asText();

            appointmentRepository.findById(appointmentId).ifPresent(a -> {
                a.setVideoSessionLink(sessionLink);
                a.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(a);
                log.info("Video link saved for appointment {}", appointmentId);
            });
        } catch (Exception e) {
            log.error("Error processing telemedicine.session.started: {}", e.getMessage());
        }
    }
}