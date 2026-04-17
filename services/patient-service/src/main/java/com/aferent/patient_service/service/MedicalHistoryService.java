package com.aferent.patient_service.service;

import com.aferent.patient_service.exception.ResourceNotFoundException;
import com.aferent.patient_service.model.MedicalHistory;
import com.aferent.patient_service.model.MedicalRecord;
import com.aferent.patient_service.repository.MedicalHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;

    /**
     * Add a prescription-issued event to a patient's medical history.
     * Creates a new medical history document if one doesn't exist for the patient.
     */
    public void recordPrescription(String patientId,
                                   String prescriptionId,
                                   String appointmentId,
                                   String consultationType,
                                   String doctorId,
                                   String doctorName,
                                   String doctorSpecialization,
                                   String hospitalId,
                                   String hospitalName,
                                   String patientName,
                                   Integer patientAge,
                                   String patientPhone,
                                   String patientEmail,
                                   String diagnosis,
                                   List<String> symptoms,
                                   List<Map<String, Object>> medications,
                                   String notes,
                                   String followUpDate,
                                   String qrCodeKey,
                                   String issuedAt) {

        // Find existing or create new medical history
        MedicalHistory medicalHistory = medicalHistoryRepository
            .findByPatientId(patientId)
            .orElseGet(() -> MedicalHistory.builder()
                .patientId(patientId)
                .build());

        // Create medical record from prescription data
        MedicalRecord record = MedicalRecord.builder()
            .prescriptionId(prescriptionId)
            .appointmentId(appointmentId)
            .consultationType(consultationType)
            .doctorId(doctorId)
            .doctorName(doctorName)
            .doctorSpecialization(doctorSpecialization)
            .hospitalId(hospitalId)
            .hospitalName(hospitalName)
            .patientId(patientId)
            .patientName(patientName)
            .patientAge(patientAge)
            .patientPhone(patientPhone)
            .patientEmail(patientEmail)
            .diagnosis(diagnosis)
            .symptoms(symptoms)
            .medications(medications)
            .notes(notes)
            .followUpDate(followUpDate)
            .qrCodeKey(qrCodeKey)
            .issuedAt(issuedAt != null ? LocalDateTime.parse(issuedAt) : LocalDateTime.now())
            .recordedAt(LocalDateTime.now())
            .build();

        // Add record to history
        medicalHistory.addRecord(record);

        // Save updated medical history
        medicalHistoryRepository.save(medicalHistory);

        log.info("Recorded prescription prescriptionId={} for patientId={}", prescriptionId, patientId);
    }

    /**
     * Retrieve all medical history records for a patient
     */
    public MedicalHistory getMedicalHistoryByPatientId(String patientId) {
        return medicalHistoryRepository.findByPatientId(patientId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Medical history not found for patient: " + patientId));
    }

    /**
     * Retrieve all medical records (without wrapper) for a patient
     */
    public List<MedicalRecord> getMedicalRecordsByPatientId(String patientId) {
        MedicalHistory history = getMedicalHistoryByPatientId(patientId);
        return history.getRecords() != null ? history.getRecords() : List.of();
    }

    /**
     * Get count of medical records for a patient
     */
    public long getRecordCountByPatientId(String patientId) {
        MedicalHistory history = medicalHistoryRepository.findByPatientId(patientId)
            .orElse(null);
        return history != null && history.getRecords() != null ? history.getRecords().size() : 0;
    }

    /**
     * Check if medical history exists for patient
     */
    public boolean medicalHistoryExists(String patientId) {
        return medicalHistoryRepository.findByPatientId(patientId).isPresent();
    }
}
