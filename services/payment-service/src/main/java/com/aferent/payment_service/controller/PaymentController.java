package com.aferent.payment_service.controller;

import com.aferent.payment_service.dto.InitiatePaymentRequest;
import com.aferent.payment_service.dto.PaymentResponse;
import com.aferent.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Patient initiates payment after booking
    // POST /payments/initiate
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("X-User-ID") String patientId,
            @RequestHeader(value = "X-User-Email", defaultValue = "") String patientEmail,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @Valid @RequestBody InitiatePaymentRequest request) {

        if (!"PATIENT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(patientId, patientEmail, request));
    }

    // Stripe calls this — NO JWT required (whitelisted in gateway)
    // Stripe sends raw body + Stripe-Signature header
    // POST /payments/webhook
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        paymentService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok(Map.of("received", "true"));
    }

    // Patient gets their own payment by appointment ID
    // GET /payments/appointment/{appointmentId}
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<PaymentResponse> getByAppointmentId(
            @PathVariable String appointmentId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {
        return ResponseEntity.ok(
                paymentService.getByAppointmentId(appointmentId, userId, role));
    }

    // Get payment by payment ID
    // GET /payments/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {
        return ResponseEntity.ok(
                paymentService.getById(id, userId, role));
    }

    // Patient views their payment history
    // GET /payments
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "PATIENT") String role) {

        if ("ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(paymentService.getAllPayments());
        }
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }

    // Admin manually triggers a refund
    // POST /payments/{id}/refund
    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> adminRefund(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {

        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(paymentService.adminRefund(id));
    }
}