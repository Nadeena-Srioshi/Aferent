package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.UpdateProfileRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorProfileService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final MinioService minioService;

    public List<Doctor> getAllActiveDoctors() {
        return doctorRepository.findByStatus(Doctor.RegistrationStatus.ACTIVE);
    }

    public Doctor getProfile(String doctorId) {
        return doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));
    }

    public Doctor updateProfile(String doctorId, String authId,
                                UpdateProfileRequest request) {
        Doctor doctor = validateOwnershipAndActive(doctorId, authId);

        if (request.getFirstName() != null)
            doctor.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            doctor.setLastName(request.getLastName());

        if (request.getPhone() != null)
            doctor.setPhone(request.getPhone());

        if (request.getBio() != null)
            doctor.setBio(request.getBio());

        if (request.getLanguages() != null)
            doctor.setLanguages(request.getLanguages());

        if (request.getYearsOfExperience() != null)
            doctor.setYearsOfExperience(request.getYearsOfExperience());

        if (request.getQualifications() != null)
            doctor.setQualifications(request.getQualifications());

        if (request.getHospitalIds() != null) {
            // validate every hospitalId exists in master collection
            for (String hospitalId : request.getHospitalIds()) {
                if (!hospitalRepository.existsById(hospitalId)) {
                    throw new IllegalArgumentException(
                            "Invalid hospitalId: " + hospitalId
                            + ". Select from the hospitals list.");
                }
            }
            doctor.setHospitals(request.getHospitalIds());
        }

        Doctor saved = doctorRepository.save(doctor);
        log.info("Profile updated for doctorId={}", doctorId);
        return saved;
    }

    public String getProfilePicUploadUrl(String doctorId, String authId,
                                          String fileName, String contentType) {
        validateOwnershipAndActive(doctorId, authId);

        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only image files are allowed for profile picture");
        }

        String objectKey = "profile-pics/" + doctorId + "/" + fileName;
        return minioService.generateUploadUrl(objectKey, contentType);
    }

    public Doctor confirmProfilePicUpload(String doctorId, String authId, String objectKey) {
        Doctor doctor = validateOwnershipAndActive(doctorId, authId);
        doctor.setProfilePicKey(objectKey);
        Doctor saved = doctorRepository.save(doctor);
        log.info("Profile picture updated for doctorId={}", doctorId);
        return saved;
    }

    public String getProfilePicDownloadUrl(String doctorId) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (doctor.getProfilePicKey() == null || doctor.getProfilePicKey().isBlank()) {
            throw new ResourceNotFoundException(
                    "No profile picture uploaded for doctorId=" + doctorId);
        }

        return minioService.generateDownloadUrl(doctor.getProfilePicKey());
    }

    private Doctor validateOwnershipAndActive(String doctorId, String authId) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (!doctor.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException(
                    "You can only manage your own profile");
        }

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException(
                    "Account not active. Current status: " + doctor.getStatus());
        }

        return doctor;
    }
}