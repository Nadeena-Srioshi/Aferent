package com.aferent.appointment_service.controller;

import com.aferent.appointment_service.dto.DoctorScheduleDto;
import com.aferent.appointment_service.model.GeneratedSlot;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import com.aferent.appointment_service.service.SlotGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug controller for testing slot generation
 * Remove this in production or protect with admin-only access
 */
@Slf4j
@RestController
@RequestMapping("/debug/slots")
@RequiredArgsConstructor
public class Slotdebugcontroller {

    private final GeneratedSlotRepository slotRepository;
    private final SlotGenerationService slotGenerationService;
    private final WebClient.Builder webClientBuilder;

    /**
     * Check all generated slots for a doctor
     * GET /debug/slots/doctor/{doctorId}
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getSlotsByDoctor(@PathVariable String doctorId) {
        List<GeneratedSlot> slots = slotRepository.findByDoctorId(doctorId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctorId);
        response.put("totalSlots", slots.size());
        response.put("bookedSlots", slots.stream().filter(GeneratedSlot::isBooked).count());
        response.put("availableSlots", slots.stream().filter(s -> !s.isBooked()).count());
        response.put("slots", slots);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check slots for a specific schedule and date
     * GET /debug/slots/schedule/{scheduleId}/date/{date}
     */
    @GetMapping("/schedule/{scheduleId}/date/{date}")
    public ResponseEntity<?> getSlotsByScheduleAndDate(
            @PathVariable String scheduleId,
            @PathVariable String date) {
        
        LocalDate localDate = LocalDate.parse(date);
        List<GeneratedSlot> slots = slotRepository.findByScheduleIdAndDateAndBookedFalse(
                scheduleId, localDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("scheduleId", scheduleId);
        response.put("date", date);
        response.put("totalSlots", slots.size());
        response.put("slots", slots);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check slots for a doctor on a specific date
     * GET /debug/slots/doctor/{doctorId}/date/{date}
     */
    @GetMapping("/doctor/{doctorId}/date/{date}")
    public ResponseEntity<?> getSlotsByDoctorAndDate(
            @PathVariable String doctorId,
            @PathVariable String date) {
        
        LocalDate localDate = LocalDate.parse(date);
        List<GeneratedSlot> slots = slotRepository.findByDoctorIdAndDate(doctorId, localDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctorId);
        response.put("date", date);
        response.put("totalSlots", slots.size());
        response.put("bookedSlots", slots.stream().filter(GeneratedSlot::isBooked).count());
        response.put("availableSlots", slots.stream().filter(s -> !s.isBooked()).count());
        response.put("slots", slots);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger slot generation for a doctor's schedule
     * POST /debug/slots/generate/doctor/{doctorId}
     */
    @PostMapping("/generate/doctor/{doctorId}")
    public ResponseEntity<?> generateSlotsForDoctor(@PathVariable String doctorId) {
        try {
            // Fetch the doctor's schedule from doctor-service
            String doctorServiceUrl = System.getenv("DOCTOR_SERVICE_URL");
            if (doctorServiceUrl == null) {
                doctorServiceUrl = "http://doctor-service:3003";
            }
            
            WebClient webClient = webClientBuilder.baseUrl(doctorServiceUrl).build();
            
            DoctorScheduleDto schedule = webClient.get()
                    .uri("/doctors/{doctorId}/schedule/weekly", doctorId)
                    .retrieve()
                    .bodyToMono(DoctorScheduleDto.class)
                    .block();
            
            if (schedule == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "No schedule found for doctor " + doctorId));
            }
            
            log.info("Fetched schedule for doctor {}: {}", doctorId, schedule);
            
            // Generate slots
            slotGenerationService.regenerateSlotsForSchedule(schedule);
            
            // Count generated slots
            List<GeneratedSlot> slots = slotRepository.findByDoctorId(doctorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Slots generated successfully");
            response.put("doctorId", doctorId);
            response.put("scheduleId", schedule.getId());
            response.put("totalSlotsGenerated", slots.size());
            response.put("preview", slots.stream().limit(5).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating slots for doctor {}: {}", doctorId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to generate slots: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger slot generation from schedule JSON
     * POST /debug/slots/generate/manual
     * Body: DoctorScheduleDto JSON
     */
    @PostMapping("/generate/manual")
    public ResponseEntity<?> generateSlotsManual(@RequestBody DoctorScheduleDto schedule) {
        try {
            log.info("Manually triggering slot generation for schedule {}", schedule.getId());
            
            slotGenerationService.regenerateSlotsForSchedule(schedule);
            
            List<GeneratedSlot> slots = slotRepository.findByDoctorId(schedule.getDoctorId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Slots generated successfully");
            response.put("scheduleId", schedule.getId());
            response.put("doctorId", schedule.getDoctorId());
            response.put("totalSlotsGenerated", slots.size());
            response.put("preview", slots.stream().limit(10).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error manually generating slots: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to generate slots: " + e.getMessage()));
        }
    }

    /**
     * Delete all slots for a doctor (for testing)
     * DELETE /debug/slots/doctor/{doctorId}
     */
    @DeleteMapping("/doctor/{doctorId}")
    public ResponseEntity<?> deleteSlotsByDoctor(@PathVariable String doctorId) {
        List<GeneratedSlot> slots = slotRepository.findByDoctorId(doctorId);
        slotRepository.deleteAll(slots);
        
        return ResponseEntity.ok(Map.of(
            "message", "Deleted all slots for doctor " + doctorId,
            "deletedCount", slots.size()
        ));
    }

    /**
     * Get statistics about slot generation
     * GET /debug/slots/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        List<GeneratedSlot> allSlots = slotRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSlots", allSlots.size());
        stats.put("bookedSlots", allSlots.stream().filter(GeneratedSlot::isBooked).count());
        stats.put("availableSlots", allSlots.stream().filter(s -> !s.isBooked()).count());
        stats.put("physicalSlots", allSlots.stream()
                .filter(s -> s.getType().toString().equals("PHYSICAL")).count());
        stats.put("videoSlots", allSlots.stream()
                .filter(s -> s.getType().toString().equals("VIDEO")).count());
        
        return ResponseEntity.ok(stats);
    }
}