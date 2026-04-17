package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.VerifyDoctorRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.kafka.DoctorEventProducer;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorEventProducer doctorEventProducer;

    @Value("${app.doctor-login-url:http://localhost:5173/login}")
    private String doctorLoginUrl;

    public List<Doctor> getPendingDoctors() {
        return doctorRepository.findByStatus(Doctor.RegistrationStatus.PENDING_VERIFICATION);
    }

    public Doctor verifyDoctor(String doctorId, VerifyDoctorRequest request, String adminRole) {

        // only ADMIN role can call this
        if (!"ADMIN".equals(adminRole)) {
            throw new ForbiddenOperationException("Only admins can verify doctors");
        }

        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        // can only verify doctors that are pending
        if (doctor.getStatus() != Doctor.RegistrationStatus.PENDING_VERIFICATION) {
            throw new ForbiddenOperationException(
                    "Doctor is not in PENDING_VERIFICATION state. Current status: " + doctor.getStatus());
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            doctor.setStatus(Doctor.RegistrationStatus.ACTIVE);
            log.info("Doctor APPROVED by admin. doctorId={}", doctorId);
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            doctor.setStatus(Doctor.RegistrationStatus.SUSPENDED);
            log.info("Doctor REJECTED by admin. doctorId={} reason={}", doctorId, request.getReason());
        } else {
            throw new IllegalArgumentException("Invalid action. Use APPROVE or REJECT");
        }

        Doctor savedDoctor = doctorRepository.save(doctor);

        // send event to Auth Service
        doctorEventProducer.sendVerificationEvent(
                savedDoctor.getAuthId(),
                request.getAction().toUpperCase()
        );

        publishVerificationNotification(savedDoctor, request);

        return savedDoctor;
    }

    private void publishVerificationNotification(Doctor doctor, VerifyDoctorRequest request) {
        if (doctor.getEmail() == null || doctor.getEmail().isBlank()) {
            log.warn("Skipping doctor verification notification due to missing email. doctorId={}", doctor.getDoctorId());
            return;
        }

        String action = request.getAction().toUpperCase();
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceService", "doctor-service");
        payload.put("channel", "EMAIL");
        payload.put("toEmail", doctor.getEmail());

        if ("APPROVE".equals(action)) {
            payload.put("eventType", "doctor.verification.approved");
            payload.put("subject", "Your Doctor Account Has Been Verified");
            payload.put("message", buildApprovedMessage(doctor));
        } else {
            payload.put("eventType", "doctor.verification.rejected");
            payload.put("subject", "Update on Your Doctor Verification");
            payload.put("message", buildRejectedMessage(doctor, request.getReason()));
        }

        try {
            doctorEventProducer.sendNotification(payload);
            log.info("Published doctor verification notification. doctorId={} action={}", doctor.getDoctorId(), action);
        } catch (Exception ex) {
            log.error("Failed to publish doctor verification notification. doctorId={} action={}", doctor.getDoctorId(), action, ex);
        }
    }

    private String buildApprovedMessage(Doctor doctor) {
        String firstName = doctor.getFirstName() != null && !doctor.getFirstName().isBlank()
                ? doctor.getFirstName()
                : "Doctor";

        return "Hello Dr. " + firstName + ",\n\n"
                + "Your profile has been verified and approved by the admin team. "
                + "You can now log in and start using the platform.\n\n"
                + "Login here: " + doctorLoginUrl + "\n\n"
                + "If you did not expect this update, please contact support.\n\n"
                + "Regards,\nAferent Team";
    }

    private String buildRejectedMessage(Doctor doctor, String reason) {
        String firstName = doctor.getFirstName() != null && !doctor.getFirstName().isBlank()
                ? doctor.getFirstName()
                : "Doctor";

        String rejectionReason = (reason != null && !reason.isBlank())
                ? reason
                : "No specific reason was provided.";

        return "Hello Dr. " + firstName + ",\n\n"
                + "Your profile verification request was reviewed, but it was not approved at this time.\n"
                + "Reason: " + rejectionReason + "\n\n"
                + "Please update your details and resubmit, or contact support for help.\n\n"
                + "Regards,\nAferent Team";
    }
}