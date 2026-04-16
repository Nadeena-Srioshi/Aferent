package com.aferent.payment_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    // Appointment reference
    private String appointmentId;
    private String appointmentType;   // PHYSICAL or VIDEO

    // Patient reference
    private String patientId;
    private String patientEmail;

    // Doctor reference
    private String doctorId;

    // Payment details
    private PaymentStatus status;
    private double amount;
    private String currency;          // "usd"

    // Stripe references
    private String stripeSessionId;       // Stripe Checkout Session ID
    private String stripePaymentIntentId; // Stripe PaymentIntent ID
    private String stripeRefundId;        // Stripe Refund ID (if refunded)

    // Checkout URL — returned to frontend to redirect patient
    private String checkoutUrl;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}