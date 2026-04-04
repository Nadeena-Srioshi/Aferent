package com.aferent.payment_service.repository;

import com.aferent.payment_service.model.Payment;
import com.aferent.payment_service.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByAppointmentId(String appointmentId);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Payment> findByPatientId(String patientId);

    List<Payment> findByDoctorId(String doctorId);

    List<Payment> findByStatus(PaymentStatus status);

    // Admin: all payments newest first
    List<Payment> findAllByOrderByCreatedAtDesc();
}