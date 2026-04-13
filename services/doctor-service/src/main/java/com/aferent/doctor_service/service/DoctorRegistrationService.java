package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.RegisterProfileRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorRegistrationService {

    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;

    public void createPendingProfile(String authId, String email) {
        if (doctorRepository.findByAuthId(authId).isPresent()) {
            log.warn("Doctor profile already exists for authId={}, skipping", authId);
            return;
        }

        String doctorId = generateDoctorId();

        Doctor doctor = Doctor.builder()
                .authId(authId)
                .doctorId(doctorId)
                .email(email)
                .status(Doctor.RegistrationStatus.PENDING_PROFILE)
                .build();

        doctorRepository.save(doctor);
        log.info("Created pending doctor profile doctorId={} for authId={}", doctorId, authId);
    }

    public Doctor completeProfile(RegisterProfileRequest request) {
        Doctor doctor = doctorRepository.findByAuthId(request.getAuthId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No doctor account found for authId=" + request.getAuthId()));

        if (doctor.getStatus() != Doctor.RegistrationStatus.PENDING_PROFILE) {
            throw new ForbiddenOperationException(
                    "Profile already submitted. Current status: " + doctor.getStatus());
        }

        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException(
                    "License number already registered: " + request.getLicenseNumber());
        }

        // validate specialization ID exists in master collection
        if (!specializationRepository.existsById(request.getSpecialization())) {
            throw new IllegalArgumentException(
                    "Invalid specialization. Select from the specializations list.");
        }

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setYearsOfExperience(request.getYearsOfExperience());
        doctor.setQualifications(request.getQualifications());
        doctor.setStatus(Doctor.RegistrationStatus.PENDING_VERIFICATION);

        Doctor saved = doctorRepository.save(doctor);
        log.info("Doctor profile completed, now PENDING_VERIFICATION. doctorId={}", saved.getDoctorId());
        return saved;
    }

    public Doctor saveLicenseKey(String authId, String objectKey) {
        Doctor doctor = doctorRepository.findByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found for authId=" + authId));
        doctor.setLicenseDocKey(objectKey);
        return doctorRepository.save(doctor);
    }

    private String generateDoctorId() {
        long count = doctorRepository.count();
        return String.format("DOC_%03d", count + 1);
    }
}