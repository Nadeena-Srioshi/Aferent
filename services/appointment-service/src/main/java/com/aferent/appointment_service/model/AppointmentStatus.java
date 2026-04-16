package com.aferent.appointment_service.model;

public enum AppointmentStatus {
    PENDING_PAYMENT,           // Physical: booked, awaiting payment
    PENDING_DOCTOR_APPROVAL,   // Video: waiting for doctor to accept
    REJECTED,                  // Video: doctor rejected
    ACCEPTED_PENDING_PAYMENT,  // Video: doctor accepted, awaiting payment
    CONFIRMED,                 // Payment done, appointment confirmed
    COMPLETED,                 // Session done
    CANCELLED,                 // Cancelled with refund
    CANCELLED_NO_REFUND,       // Cancelled too late, no refund
    PAYMENT_FAILED             // Payment attempt failed
}