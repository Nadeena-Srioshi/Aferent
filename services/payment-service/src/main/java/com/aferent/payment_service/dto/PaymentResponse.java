package com.aferent.payment_service.dto;

import com.aferent.payment_service.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private PaymentStatus status;
    private double amount;
    private String currency;
    private String checkoutUrl;        // redirect patient here to pay
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String stripeRefundId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}