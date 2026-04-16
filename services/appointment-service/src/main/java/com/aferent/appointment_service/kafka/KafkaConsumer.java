// package com.aferent.appointment_service.kafka;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.aferent.appointment_service.dto.DoctorScheduleDto;
// import com.aferent.appointment_service.model.Appointment;
// import com.aferent.appointment_service.model.AppointmentStatus;
// import com.aferent.appointment_service.repository.AppointmentRepository;
// import com.aferent.appointment_service.repository.GeneratedSlotRepository;
// import com.aferent.appointment_service.service.SlotGenerationService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.Optional;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class KafkaConsumer {

//     private final AppointmentRepository   appointmentRepository;
//     private final GeneratedSlotRepository slotRepository;
//     private final SlotGenerationService   slotGenerationService;
//     private final KafkaProducer           kafkaProducer;
//     private final ObjectMapper            objectMapper;

//     // Friend's Doctor Service emits this when schedule is created/updated/deleted
//     // You consume it and regenerate slots
//     @KafkaListener(topics = "availability.updated", groupId = "appointment-service")
//     public void onAvailabilityUpdated(String message) {
//         try {
//             DoctorScheduleDto schedule = objectMapper.readValue(
//                     message, DoctorScheduleDto.class);
//             log.info("availability.updated received scheduleId={}", schedule.getId());

//             if (!schedule.isActive()) {
//                 // Doctor deleted this schedule — remove all future unbooked slots
//                 slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
//                         schedule.getId(), java.time.LocalDate.now());
//                 log.info("Slots deleted for inactive scheduleId={}", schedule.getId());
//             } else {
//                 slotGenerationService.regenerateSlotsForSchedule(schedule);
//             }
//         } catch (Exception e) {
//             log.error("Error processing availability.updated: {}", e.getMessage());
//         }
//     }

//     // Payment service confirms payment succeeded → set appointment CONFIRMED
//     @KafkaListener(topics = "payment.success", groupId = "appointment-service")
//     public void onPaymentSuccess(String message) {
//         try {
//             JsonNode node = objectMapper.readTree(message);
//             String appointmentId = node.get("appointmentId").asText();
//             String paymentId     = node.get("paymentId").asText();

//             Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
//             if (opt.isPresent()) {
//                 Appointment a = opt.get();
//                 a.setStatus(AppointmentStatus.CONFIRMED);
//                 a.setPaymentId(paymentId);
//                 a.setConfirmedAt(LocalDateTime.now());
//                 a.setUpdatedAt(LocalDateTime.now());
//                 appointmentRepository.save(a);

//                 // Tell Doctor Service to evict Redis slot
//                 kafkaProducer.publishSlotBooked(a);

//                 // Tell Notification Service to send confirmation SMS/email
//                 kafkaProducer.publishAppointmentConfirmed(a);

//                 log.info("Appointment {} CONFIRMED after payment success", appointmentId);
//             }
//         } catch (Exception e) {
//             log.error("Error processing payment.success: {}", e.getMessage());
//         }
//     }

//     // Payment failed → mark appointment, release slot back
//     @KafkaListener(topics = "payment.failed", groupId = "appointment-service")
//     public void onPaymentFailed(String message) {
//         try {
//             JsonNode node = objectMapper.readTree(message);
//             String appointmentId = node.get("appointmentId").asText();

//             Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
//             if (opt.isPresent()) {
//                 Appointment a = opt.get();
//                 a.setStatus(AppointmentStatus.PAYMENT_FAILED);
//                 a.setUpdatedAt(LocalDateTime.now());
//                 appointmentRepository.save(a);

//                 // Release slot so next patient can take it
//                 if (a.getGeneratedSlotId() != null) {
//                     slotRepository.findById(a.getGeneratedSlotId()).ifPresent(slot -> {
//                         slot.setBooked(false);
//                         slotRepository.save(slot);
//                     });
//                 }
//                 log.info("Appointment {} PAYMENT_FAILED — slot released", appointmentId);
//             }
//         } catch (Exception e) {
//             log.error("Error processing payment.failed: {}", e.getMessage());
//         }
//     }

//     // Telemedicine service generates video link — save it to appointment
//     @KafkaListener(topics = "telemedicine.session.started", groupId = "appointment-service")
//     public void onTelemedicineSession(String message) {
//         try {
//             JsonNode node = objectMapper.readTree(message);
//             String appointmentId = node.get("appointmentId").asText();
//             String sessionLink   = node.get("sessionLink").asText();

//             appointmentRepository.findById(appointmentId).ifPresent(a -> {
//                 a.setVideoSessionLink(sessionLink);
//                 a.setUpdatedAt(LocalDateTime.now());
//                 appointmentRepository.save(a);
//                 log.info("Video link saved for appointment {}", appointmentId);
//             });
//         } catch (Exception e) {
//             log.error("Error processing telemedicine.session.started: {}", e.getMessage());
//         }
//     }
// }

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
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
    // @KafkaListener(topics = "availability.updated", groupId = "appointment-service")
    // public void onAvailabilityUpdated(String message) {
    //     try {
    //         DoctorScheduleDto schedule = objectMapper.readValue(
    //                 message, DoctorScheduleDto.class);
    //         log.info("availability.updated received scheduleId={}", schedule.getId());

    //         if (!schedule.isActive()) {
    //             // Doctor deleted this schedule — remove all future unbooked slots
    //             slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
    //                     schedule.getId(), java.time.LocalDate.now());
    //             log.info("Slots deleted for inactive scheduleId={}", schedule.getId());
    //         } else {
    //             slotGenerationService.regenerateSlotsForSchedule(schedule);
    //         }
    //     } catch (Exception e) {
    //         log.error("Error processing availability.updated: {}", e.getMessage());
    //     }
    // }

    @KafkaListener(topics = "doctor.schedule.weekly.upserted", groupId = "appointment-service")
    public void onDoctorWeeklyUpserted(String message) {
        try {
            DoctorScheduleDto schedule = objectMapper.readValue(message, DoctorScheduleDto.class);
            slotGenerationService.regenerateSlotsForSchedule(schedule);
            log.info("doctor.schedule.weekly.upserted handled scheduleId={}", schedule.getId());
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.weekly.upserted: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "doctor.schedule.weekly.deleted", groupId = "appointment-service")
    public void onDoctorWeeklyDeleted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String scheduleId = node.get("id").asText();
            slotRepository.deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
                    scheduleId,
                    LocalDate.now()
            );
            log.info("doctor.schedule.weekly.deleted handled scheduleId={}", scheduleId);
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.weekly.deleted: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "doctor.schedule.override.upserted", groupId = "appointment-service")
    public void onDoctorOverrideUpserted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String action = node.get("overrideAction").asText();
            String doctorId = node.get("doctorId").asText();
            LocalDate date = LocalDate.parse(node.get("date").asText());

            if ("BLOCK".equals(action)) {
                slotRepository.deleteByDoctorIdAndDateAndBookedFalse(doctorId, date);
                log.info("Override BLOCK handled doctorId={} date={}", doctorId, date);
                return;
            }

            if ("ADD".equals(action)) {
                DoctorScheduleDto schedule = objectMapper.treeToValue(node, DoctorScheduleDto.class);
                slotGenerationService.generateSlotsForScheduleOnDate(schedule, date, false);
                log.info("Override ADD handled scheduleId={} date={}", schedule.getId(), date);
                return;
            }

            if ("CANCEL_SESSION".equals(action)) {
                String sessionId = node.get("sessionId").asText();
                slotRepository.deleteByScheduleIdAndDateAndBookedFalse(sessionId, date);
                log.info("Override CANCEL_SESSION handled sessionId={} date={}", sessionId, date);
            }
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.override.upserted: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "doctor.schedule.override.deleted", groupId = "appointment-service")
    public void onDoctorOverrideDeleted(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String deletedAction = node.get("deletedAction").asText();
            LocalDate date = LocalDate.parse(node.get("date").asText());

            if ("ADD".equals(deletedAction) && node.hasNonNull("id")) {
                String scheduleId = node.get("id").asText();
                slotRepository.deleteByScheduleIdAndDateAndBookedFalse(scheduleId, date);
                log.info("Override delete for ADD handled scheduleId={} date={}", scheduleId, date);
                return;
            }

            if (("BLOCK".equals(deletedAction) || "CANCEL_SESSION".equals(deletedAction))
                    && node.has("restoreSchedules")) {
                Iterator<JsonNode> iterator = node.get("restoreSchedules").elements();
                while (iterator.hasNext()) {
                    JsonNode restoreNode = iterator.next();
                    DoctorScheduleDto schedule = objectMapper.treeToValue(restoreNode, DoctorScheduleDto.class);
                    slotGenerationService.generateSlotsForScheduleOnDate(schedule, date, true);
                }
                log.info("Override delete restore handled action={} date={}", deletedAction, date);
            }
        } catch (Exception e) {
            log.error("Error processing doctor.schedule.override.deleted: {}", e.getMessage());
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