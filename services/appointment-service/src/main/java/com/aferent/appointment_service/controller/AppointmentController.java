package com.aferent.appointment_service.controller;

import com.aferent.appointment_service.dto.AppointmentResponse;
import com.aferent.appointment_service.dto.BookAppointmentRequest;
import com.aferent.appointment_service.dto.SlotResponse;
import com.aferent.appointment_service.model.AppointmentType;
import com.aferent.appointment_service.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ── SLOTS (read-only, for patient browsing) ───────────────────────────────

    // Patient browses available slots for a schedule on a specific date
    // GET /appointments/slots?scheduleId=xxx&date=2025-04-07
    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @RequestParam String scheduleId,
            @RequestParam String date) {
        return ResponseEntity.ok(
                appointmentService.getAvailableSlotsBySchedule(scheduleId, date));
    }

        // Patient browses available slots for a doctor.
        // doctorId is required; type and date are optional filters.
        // GET /appointments/slots/doctor/{doctorId}
        // GET /appointments/slots/doctor/{doctorId}?date=2025-04-07
        // GET /appointments/slots/doctor/{doctorId}?type=PHYSICAL&date=2025-04-07
    @GetMapping("/slots/doctor/{doctorId}")
    public ResponseEntity<List<SlotResponse>> getSlotsByDoctor(
            @PathVariable String doctorId,
            @RequestParam(required = false) AppointmentType type,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(
                appointmentService.getAvailableSlots(doctorId, type, date));
    }

    // ── BOOK ─────────────────────────────────────────────────────────────────

    // Patient books an appointment (physical or video)
    // POST /appointments
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @RequestHeader("X-User-ID")    String patientId,
            @RequestHeader(value = "X-User-Email",  defaultValue = "") String email,
            @RequestHeader(value = "X-User-Role",   defaultValue = "") String role,
            @Valid @RequestBody BookAppointmentRequest request) {
        

        if (!"PATIENT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(patientId, email, request));
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    // Get all appointments for the logged-in user
    // GET /appointments
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAll(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {
        return ResponseEntity.ok(
                appointmentService.getMyAppointments(userId, role));
    }

    // Get a single appointment by ID
    // GET /appointments/{id}
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {
        return ResponseEntity.ok(
                appointmentService.getById(id, userId, role));
    }

    // Doctor gets their pending video requests
    // GET /appointments/pending-video
    @GetMapping("/pending-video")
    public ResponseEntity<List<AppointmentResponse>> pendingVideo(
            @RequestHeader("X-User-ID") String doctorId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {

        if (!"DOCTOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
                appointmentService.getPendingVideoRequests(doctorId));
    }

    // ── STATUS UPDATES ────────────────────────────────────────────────────────

    // Internal — Doctor Service calls this to accept/reject video appointment
    // PATCH /appointments/{id}/status
    // Body: { "status": "ACCEPTED_PENDING_PAYMENT" } or { "status": "REJECTED" }
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-ID") String callerId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {

        if (!"DOCTOR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                appointmentService.updateStatus(id, newStatus, callerId));
    }

    // Patient cancels an appointment
    // PATCH /appointments/{id}/cancel
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable String id,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {
        return ResponseEntity.ok(
                appointmentService.cancelAppointment(id, userId, role));
    }

    // Doctor marks appointment as completed after session
    // PATCH /appointments/{id}/complete
    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(
            @PathVariable String id,
            @RequestHeader("X-User-ID") String doctorId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {

        if (!"DOCTOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
                appointmentService.completeAppointment(id, doctorId));
    }
}