package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.VerifyDoctorRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.kafka.DoctorEventProducer;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorEventProducer doctorEventProducer;

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

        return savedDoctor;
    }
}