package com.aferent.payment_service.service;

import com.aferent.payment_service.dto.InitiatePaymentRequest;
import com.aferent.payment_service.dto.PaymentResponse;
import com.aferent.payment_service.exception.AppException;
import com.aferent.payment_service.kafka.KafkaProducer;
import com.aferent.payment_service.model.Payment;
import com.aferent.payment_service.model.PaymentStatus;
import com.aferent.payment_service.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaProducer kafkaProducer;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.currency}")
    private String currency;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    // ─── INITIATE ────────────────────────────────────────────────────────────

    public PaymentResponse initiatePayment(
            String patientId, String patientEmail,
            InitiatePaymentRequest request) {

        // Prevent duplicate payment sessions for same appointment
        paymentRepository.findByAppointmentId(request.getAppointmentId())
                .ifPresent(existing -> {
                    if (existing.getStatus() == PaymentStatus.PENDING
                            || existing.getStatus() == PaymentStatus.SUCCESS) {
                        throw new AppException(
                                "Payment already exists for this appointment",
                                HttpStatus.CONFLICT);
                    }
                });

        try {
            // Build Stripe Checkout Session
            // Amount in Stripe must be in cents (smallest currency unit)
            long amountInCents = Math.round(request.getAmount() * 100);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(patientEmail)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(amountInCents)
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData
                                            .ProductData.builder()
                                            .setName("Healthcare Consultation")
                                            .setDescription(
                                                request.getAppointmentType()
                                                + " appointment - ID: "
                                                + request.getAppointmentId())
                                            .build())
                                    .build())
                            .build())
                    // Store appointmentId in metadata — we read this in webhook
                    .putMetadata("appointmentId", request.getAppointmentId())
                    .putMetadata("patientId",     patientId)
                    .putMetadata("doctorId",
                            request.getDoctorId() != null ? request.getDoctorId() : "")
                    .build();

            Session session = Session.create(params);

            // Save payment record
            Payment payment = Payment.builder()
                    .appointmentId(request.getAppointmentId())
                    .appointmentType(request.getAppointmentType())
                    .patientId(patientId)
                    .patientEmail(patientEmail)
                    .doctorId(request.getDoctorId())
                    .status(PaymentStatus.PENDING)
                    .amount(request.getAmount())
                    .currency(currency)
                    .stripeSessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            payment = paymentRepository.save(payment);
            log.info("Payment initiated for appointmentId={} sessionId={}",
                    request.getAppointmentId(), session.getId());

            return toResponse(payment);

        } catch (StripeException e) {
            log.error("Stripe error initiating payment: {}", e.getMessage());
            throw new AppException(
                    "Failed to create payment session: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── STRIPE WEBHOOK ──────────────────────────────────────────────────────

    public void handleStripeWebhook(String payload, String sigHeader) {
        Event event;

        // Verify the webhook signature — this prevents fake webhook calls
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed");
            throw new AppException(
                    "Invalid webhook signature", HttpStatus.BAD_REQUEST);
        }

        log.info("Stripe webhook received: {}", event.getType());

        // Deserialize the event data object
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        switch (event.getType()) {

            case "checkout.session.completed" -> {
                Session session = (Session) deserializer
                        .getObject().orElseThrow();
                handleCheckoutCompleted(session);
            }

            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) deserializer
                        .getObject().orElseThrow();
                handlePaymentIntentSucceeded(intent);
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) deserializer
                        .getObject().orElseThrow();
                handlePaymentIntentFailed(intent);
            }

            case "checkout.session.expired" -> {
                Session session = (Session) deserializer
                        .getObject().orElseThrow();
                handleCheckoutExpired(session);
            }

            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Session session) {
        paymentRepository.findByStripeSessionId(session.getId())
                .ifPresent(payment -> {
                    payment.setStripePaymentIntentId(session.getPaymentIntent());
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    kafkaProducer.publishPaymentSuccess(payment);
                    log.info("Payment SUCCESS for appointmentId={}",
                            payment.getAppointmentId());
                });
    }

    private void handlePaymentIntentSucceeded(PaymentIntent intent) {
        // Backup handler — in case checkout.session.completed already handled it
        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresent(payment -> {
                    if (payment.getStatus() != PaymentStatus.SUCCESS) {
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setPaidAt(LocalDateTime.now());
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                        kafkaProducer.publishPaymentSuccess(payment);
                    }
                });
    }

    private void handlePaymentIntentFailed(PaymentIntent intent) {
        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    kafkaProducer.publishPaymentFailed(payment);
                    log.info("Payment FAILED for appointmentId={}",
                            payment.getAppointmentId());
                });
    }

    private void handleCheckoutExpired(Session session) {
        paymentRepository.findByStripeSessionId(session.getId())
                .ifPresent(payment -> {
                    if (payment.getStatus() == PaymentStatus.PENDING) {
                        payment.setStatus(PaymentStatus.CANCELLED);
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                        kafkaProducer.publishPaymentFailed(payment);
                        log.info("Checkout session expired for appointmentId={}",
                                payment.getAppointmentId());
                    }
                });
    }

    // ─── REFUND ──────────────────────────────────────────────────────────────

    // Called by KafkaConsumer when refund.trigger event arrives
    public void processRefundById(String paymentId, String appointmentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseGet(() ->
                    paymentRepository.findByAppointmentId(appointmentId)
                            .orElseThrow(() -> new AppException(
                                    "Payment not found for refund",
                                    HttpStatus.NOT_FOUND))
                );
        processRefund(payment);
    }

    // Admin manually triggers a refund
    public PaymentResponse adminRefund(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(
                        "Payment not found", HttpStatus.NOT_FOUND));
        return toResponse(processRefund(payment));
    }

    private Payment processRefund(Payment payment) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new AppException(
                    "Only successful payments can be refunded",
                    HttpStatus.BAD_REQUEST);
        }
        if (payment.getStripePaymentIntentId() == null) {
            throw new AppException(
                    "No Stripe payment intent found for refund",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Create refund via Stripe API
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .build();

            Refund refund = Refund.create(params);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setStripeRefundId(refund.getId());
            payment.setRefundedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            kafkaProducer.publishRefundCompleted(payment);
            log.info("Refund SUCCESS for appointmentId={} refundId={}",
                    payment.getAppointmentId(), refund.getId());

            return payment;

        } catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.SUCCESS); // roll back to SUCCESS
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            throw new AppException(
                    "Refund failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── GET METHODS ─────────────────────────────────────────────────────────

    public PaymentResponse getByAppointmentId(String appointmentId, String userId,
            String role) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new AppException(
                        "Payment not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isOwner = payment.getPatientId().equals(userId)
                || payment.getDoctorId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        return toResponse(payment);
    }

    public PaymentResponse getById(String paymentId, String userId, String role) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(
                        "Payment not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isOwner = payment.getPatientId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        return toResponse(payment);
    }

    public List<PaymentResponse> getMyPayments(String patientId) {
        return paymentRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .appointmentId(p.getAppointmentId())
                .patientId(p.getPatientId())
                .doctorId(p.getDoctorId())
                .status(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .checkoutUrl(p.getCheckoutUrl())
                .stripeSessionId(p.getStripeSessionId())
                .stripePaymentIntentId(p.getStripePaymentIntentId())
                .stripeRefundId(p.getStripeRefundId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .build();
    }
}