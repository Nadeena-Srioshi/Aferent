package com.aferent.patient_service.service;

import com.aferent.patient_service.dto.*;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.exception.ResourceNotFoundException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientIdSequence;
import com.aferent.patient_service.repository.PatientRepository;
import com.aferent.patient_service.repository.PatientIdSequenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientIdSequenceRepository patientIdSequenceRepository;

    @PostConstruct   // runs once when service starts up
    public void init() {
        // initialize patient ID sequence if not exists
        if (!patientIdSequenceRepository.existsById("patientIdCounter")) {
            PatientIdSequence sequence = PatientIdSequence.builder()
                    .id("patientIdCounter")
                    .sequence(0L)
                    .build();
            patientIdSequenceRepository.save(sequence);
            log.info("Initialized patient ID sequence counter");
        }
    }

    // helper: generate next patientId (PAT_001, PAT_002, etc.)
    private String generatePatientId() {
        PatientIdSequence sequence = patientIdSequenceRepository.findById("patientIdCounter")
            .orElseThrow(() -> new ResourceNotFoundException("Patient ID sequence not initialized"));
        sequence.setSequence(sequence.getSequence() + 1);
        PatientIdSequence updated = patientIdSequenceRepository.save(sequence);
        return String.format("PAT_%03d", updated.getSequence());  // e.g., PAT_001, PAT_002
    }

    // called by Kafka consumer when user.registered event arrives
    // creates an empty patient profile linked to the auth authId
    public Patient createProfile(String authId, String email) {
        return patientRepository.findByAuthId(authId)
                .or(() -> patientRepository.findByEmail(email))
                .orElseGet(() -> {
                    String patientId = generatePatientId();
                    Patient patient = Patient.builder()
                            .authId(authId)          // auth-service user ID
                            .patientId(patientId)    // human-readable ID (PAT_001, etc.)
                            .email(email)
                            .build();
                    log.info("Creating patient profile: authId={}, patientId={}, email={}", authId, patientId, email);
                    return patientRepository.save(patient);
                });
    }

    public Patient getProfile(String patientId) {
        return patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    public Patient getProfileByAuthId(String authId) {
        return patientRepository.findByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));
    }

    public Patient updateProfile(String patientId, String requesterId, UpdateProfileRequest req) {
        // requesterId is the authId from X-User-ID header
        // security check — patients can only update their own profile
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        
        if (!patient.getAuthId().equals(requesterId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        
        if (req.getFirstName() != null) patient.setFirstName(req.getFirstName());
        if (req.getLastName() != null)  patient.setLastName(req.getLastName());
        if (req.getPhone() != null)     patient.setPhone(req.getPhone());
        if (req.getDateOfBirth() != null) patient.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null)    patient.setGender(req.getGender());
        if (req.getBloodGroup() != null) patient.setBloodGroup(req.getBloodGroup());
        if (req.getAddress() != null)   patient.setAddress(req.getAddress());
        return patientRepository.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll().stream()
                .sorted(Comparator.comparing(Patient::getPatientId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}