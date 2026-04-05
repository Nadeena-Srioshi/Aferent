package com.aferent.payment_service.model;

public enum PaymentStatus {
    PENDING,        // Stripe session created, waiting for patient
    SUCCESS,        // Stripe confirmed payment
    FAILED,         // Stripe payment failed or expired
    REFUNDED,       // Full refund issued
    REFUND_PENDING, // Refund requested, processing
    CANCELLED       // Payment cancelled before completion
}
