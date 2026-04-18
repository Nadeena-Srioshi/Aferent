package com.aferent.appointment_service.service;

import com.aferent.appointment_service.dto.AppointmentResponse;
import com.aferent.appointment_service.dto.BookAppointmentRequest;
import com.aferent.appointment_service.dto.SlotResponse;
import com.aferent.appointment_service.exception.AppException;
import com.aferent.appointment_service.kafka.KafkaProducer;
import com.aferent.appointment_service.model.*;
import com.aferent.appointment_service.repository.AppointmentRepository;
import com.aferent.appointment_service.repository.GeneratedSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final int DOCTOR_SLOT_FUTURE_WINDOW_DAYS = 21;

    private final AppointmentRepository     appointmentRepository;
    private final GeneratedSlotRepository   slotRepository;
    private final KafkaProducer             kafkaProducer;
    private final WebClient.Builder         webClientBuilder;

    @Value("${appointment.refund-cutoff-hours}")
    private int refundCutoffHours;

    @Value("${services.doctor-service-url:http://doctor-service:3003}")
    private String doctorServiceUrl;

    // ─── BROWSE SLOTS ────────────────────────────────────────────────────────

    public List<SlotResponse> getAvailableSlots(
            String doctorId, AppointmentType type, String dateStr) {
        LocalDate selectedDate = (dateStr != null && !dateStr.isBlank())
            ? LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            : null;

        LocalDate today = LocalDate.now();
        LocalDate maxFutureDate = today.plusDays(DOCTOR_SLOT_FUTURE_WINDOW_DAYS);

        List<GeneratedSlot> slots = (selectedDate != null)
            ? slotRepository.findByDoctorIdAndDate(doctorId, selectedDate)
            : slotRepository.findByDoctorId(doctorId);

        return slots
                .stream()
                .filter(s -> !s.isBooked())
                .filter(s -> type == null || s.getType() == type)
                .filter(s -> !s.getDate().isBefore(today))
                .filter(s -> !s.getDate().isAfter(maxFutureDate))
                .sorted(Comparator
                        .comparing(GeneratedSlot::getDate)
                        .thenComparing(GeneratedSlot::getStartTime))
                .map(this::toSlotResponse)
                .collect(Collectors.toList());
    }

    public List<SlotResponse> getAvailableSlotsBySchedule(
            String scheduleId, String dateStr) {

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        return slotRepository
                .findByScheduleIdAndDateAndBookedFalse(scheduleId, date)
                .stream()
                .map(this::toSlotResponse)
                .collect(Collectors.toList());
    }

    // ─── BOOK ────────────────────────────────────────────────────────────────

    public AppointmentResponse bookAppointment(
            String patientId, String patientEmail,
            BookAppointmentRequest request) {

        LocalDate date = LocalDate.parse(
                request.getAppointmentDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        Appointment appointment;

        if (request.getType() == AppointmentType.PHYSICAL) {
            appointment = bookPhysical(patientId, patientEmail, request, date);
        } else {
            appointment = bookVideo(patientId, patientEmail, request, date);
        }

        appointment = appointmentRepository.save(appointment);
        kafkaProducer.publishAppointmentBooked(appointment);

        log.info("Appointment booked id={} type={} patient={}",
                appointment.getId(), appointment.getType(), patientId);

        return toResponse(appointment);
    }

    private Appointment bookPhysical(String patientId, String patientEmail,
            BookAppointmentRequest request, LocalDate date) {

        // Find the next available (unbooked) physical slot for this schedule+date
        List<GeneratedSlot> available = slotRepository
                .findByScheduleIdAndDateAndBookedFalse(request.getScheduleId(), date)
                .stream()
                .filter(s -> s.getType() == AppointmentType.PHYSICAL)
                .sorted((a, b) -> a.getAppointmentNumber()
                        .compareTo(b.getAppointmentNumber()))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            throw new AppException(
                "No physical slots available for this date", HttpStatus.CONFLICT);
        }

        // Always assign the next available slot number
        GeneratedSlot slot = available.get(0);
        slot.setBooked(true);
        slotRepository.save(slot);
        

        double fee = slot.getConsultationFee() != null ? slot.getConsultationFee() : 0.0;
        AppointmentStatus status = fee > 0 ? AppointmentStatus.PENDING_PAYMENT : AppointmentStatus.CONFIRMED;

        return Appointment.builder()
                .patientId(patientId)
                .patientEmail(patientEmail)
                .patientName(request.getPatientName())
                .doctorId(request.getDoctorId())
                .doctorAuthId(resolveDoctorAuthId(request.getDoctorId()))
                .doctorName(request.getDoctorName())
                .scheduleId(request.getScheduleId())
                .generatedSlotId(slot.getId())
                .type(AppointmentType.PHYSICAL)
                .status(status)
                .appointmentDate(date)
                .appointmentNumber(slot.getAppointmentNumber())
                .calculatedTime(slot.getStartTime())
                .hospitalName(slot.getHospitalName())
                .hospitalLocation(slot.getHospitalLocation())
                .consultationFee(fee)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Appointment bookVideo(String patientId, String patientEmail,
            BookAppointmentRequest request, LocalDate date) {

        if (request.getVideoSlotId() == null || request.getVideoSlotId().isBlank()) {
            throw new AppException(
                    "videoSlotId is required for VIDEO appointments",
                    HttpStatus.BAD_REQUEST);
        }

        GeneratedSlot slot = slotRepository
                .findByScheduleIdAndDateAndVideoSlotId(
                        request.getScheduleId(), date, request.getVideoSlotId())
                .orElseThrow(() -> new AppException(
                        "Video slot not found", HttpStatus.NOT_FOUND));

        if (slot.isBooked()) {
            throw new AppException(
                    "This video slot is already booked", HttpStatus.CONFLICT);
        }

        slot.setBooked(true);
        slotRepository.save(slot);

        return Appointment.builder()
                .patientId(patientId)
                .patientEmail(patientEmail)
                .patientName(request.getPatientName())
                .doctorId(request.getDoctorId())
                .doctorAuthId(resolveDoctorAuthId(request.getDoctorId()))
                .doctorName(request.getDoctorName())
                .scheduleId(request.getScheduleId())
                .generatedSlotId(slot.getId())
                .type(AppointmentType.VIDEO)
                .status(AppointmentStatus.PENDING_DOCTOR_APPROVAL)
                .appointmentDate(date)
                .videoSlotId(slot.getVideoSlotId())
                .videoSlotStart(slot.getStartTime())
                .videoSlotEnd(slot.getEndTime())
                .consultationFee(slot.getConsultationFee())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── STATUS UPDATE (called by Doctor Service proxy) ───────────────────────

    public AppointmentResponse updateStatus(
            String appointmentId, String newStatus, String callerId) {

        Appointment appointment = findById(appointmentId);

        AppointmentStatus status = AppointmentStatus.valueOf(newStatus.toUpperCase());
        appointment.setStatus(status);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment = appointmentRepository.save(appointment);

        // If doctor accepted video → fire booked again so payment-svc prompts patient
        if (status == AppointmentStatus.ACCEPTED_PENDING_PAYMENT) {
            kafkaProducer.publishAppointmentBooked(appointment);
        }

        return toResponse(appointment);
    }

    // ─── CANCEL ──────────────────────────────────────────────────────────────

    public AppointmentResponse cancelAppointment(
            String appointmentId, String userId, String role) {

        Appointment appointment = findById(appointmentId);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && !appointment.getPatientId().equals(userId)) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        AppointmentStatus current = appointment.getStatus();
        if (current == AppointmentStatus.CANCELLED
                || current == AppointmentStatus.CANCELLED_NO_REFUND
                || current == AppointmentStatus.COMPLETED) {
            throw new AppException(
                    "Cannot cancel appointment in status: " + current,
                    HttpStatus.BAD_REQUEST);
        }

        boolean refundable = isRefundEligible(appointment);

        appointment.setStatus(refundable
                ? AppointmentStatus.CANCELLED
                : AppointmentStatus.CANCELLED_NO_REFUND);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment = appointmentRepository.save(appointment);

        // Release the slot back so next patient can book it
        releaseSlot(appointment.getGeneratedSlotId());

        kafkaProducer.publishAppointmentCancelled(appointment);

        // Only trigger refund if payment was already made
        if (refundable && appointment.getPaymentId() != null) {
            kafkaProducer.publishRefundTrigger(appointment);
        }

        log.info("Appointment {} cancelled refundable={}", appointmentId, refundable);
        return toResponse(appointment);
    }

    private void releaseSlot(String slotId) {
        if (slotId == null) return;
        slotRepository.findById(slotId).ifPresent(slot -> {
            slot.setBooked(false);
            slotRepository.save(slot);
            log.info("Slot {} released back to pool", slotId);
        });
    }

    private boolean isRefundEligible(Appointment appointment) {
        // Not yet paid — no refund needed
        if (appointment.getStatus() == AppointmentStatus.PENDING_PAYMENT
                || appointment.getStatus() == AppointmentStatus.PENDING_DOCTOR_APPROVAL
                || appointment.getStatus() == AppointmentStatus.ACCEPTED_PENDING_PAYMENT
                || appointment.getStatus() == AppointmentStatus.PAYMENT_FAILED) {
            return false;
        }

        var sessionTime = appointment.getType() == AppointmentType.PHYSICAL
                ? appointment.getCalculatedTime()
                : appointment.getVideoSlotStart();

        if (sessionTime == null) return false;

        var appointmentDateTime = appointment.getAppointmentDate().atTime(sessionTime);
        return LocalDateTime.now()
                .isBefore(appointmentDateTime.minusHours(refundCutoffHours));
    }

    // ─── COMPLETE ────────────────────────────────────────────────────────────

    public AppointmentResponse completeAppointment(
            String appointmentId, String doctorId) {

        Appointment appointment = findById(appointmentId);

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppException(
                    "Only CONFIRMED appointments can be completed",
                    HttpStatus.BAD_REQUEST);
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(LocalDateTime.now());
        return toResponse(appointmentRepository.save(appointment));
    }

    // ─── GET METHODS ─────────────────────────────────────────────────────────

    public AppointmentResponse getById(
            String appointmentId, String userId, String role) {

        Appointment appointment = findById(appointmentId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isOwner = appointment.getPatientId().equals(userId)
                || appointment.getDoctorId().equals(userId)
                || (appointment.getDoctorAuthId() != null && appointment.getDoctorAuthId().equals(userId));

        if (!isAdmin && !isOwner) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        return toResponse(appointment);
    }

    public List<AppointmentResponse> getMyAppointments(String userId, String role) {
        List<Appointment> list;
        if ("ADMIN".equalsIgnoreCase(role)) {
            list = appointmentRepository.findAllByOrderByCreatedAtDesc();
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            list = appointmentRepository.findByDoctorAuthId(userId);
        } else {
            list = appointmentRepository.findByPatientId(userId);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getPendingVideoRequests(String doctorAuthId) {
        return appointmentRepository
                .findByDoctorAuthIdAndStatus(
                        doctorAuthId, AppointmentStatus.PENDING_DOCTOR_APPROVAL)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String resolveDoctorAuthId(String doctorId) {
        try {
            WebClient client = webClientBuilder.baseUrl(doctorServiceUrl).build();
            Map<String, Object> profile = client.get()
                    .uri("/doctors/{id}", doctorId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return profile != null ? (String) profile.get("authId") : null;
        } catch (Exception e) {
            log.warn("Could not resolve authId for doctorId={}: {}", doctorId, e.getMessage());
            return null;
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Appointment findById(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Appointment not found", HttpStatus.NOT_FOUND));
    }

    private SlotResponse toSlotResponse(GeneratedSlot s) {
        return SlotResponse.builder()
                .slotId(s.getId())
                .scheduleId(s.getScheduleId())
                .doctorId(s.getDoctorId())
                .type(s.getType())
                .date(s.getDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .booked(s.isBooked())
                .consultationFee(s.getConsultationFee())
                .appointmentNumber(s.getAppointmentNumber())
                .hospitalName(s.getHospitalName())
                .hospitalLocation(s.getHospitalLocation())
                .videoSlotId(s.getVideoSlotId())
                .build();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(a.getPatientName())
                .doctorId(a.getDoctorId())
                .doctorAuthId(a.getDoctorAuthId())
                .doctorName(a.getDoctorName())
                .type(a.getType())
                .status(a.getStatus())
                .appointmentDate(a.getAppointmentDate())
                .appointmentNumber(a.getAppointmentNumber())
                .calculatedTime(a.getCalculatedTime())
                .hospitalName(a.getHospitalName())
                .hospitalLocation(a.getHospitalLocation())
                .videoSlotId(a.getVideoSlotId())
                .videoSlotStart(a.getVideoSlotStart())
                .videoSlotEnd(a.getVideoSlotEnd())
                .videoSessionLink(a.getVideoSessionLink())
                .consultationFee(a.getConsultationFee())
                .paymentId(a.getPaymentId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}