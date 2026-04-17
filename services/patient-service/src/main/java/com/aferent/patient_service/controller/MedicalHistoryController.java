package com.aferent.patient_service.controller;

import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.model.MedicalHistory;
import com.aferent.patient_service.model.MedicalRecord;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.service.MedicalHistoryService;
import com.aferent.patient_service.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;
    private final PatientService patientService;

    /**
     * GET /patients/{patientId}/medical-history
     * Retrieve complete medical history for a patient
     */
    @GetMapping("/{patientId}/medical-history")
    public ResponseEntity<MedicalHistory> getMedicalHistory(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);

        // Doctors, admins can view any patient's medical history
        // Patients can only view their own
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }

        MedicalHistory history = medicalHistoryService.getMedicalHistoryByPatientId(patientId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /patients/me/medical-history
     * Retrieve medical history for the current authenticated patient
     */
    @GetMapping("/me/medical-history")
    public ResponseEntity<MedicalHistory> getCurrentMedicalHistory(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Only patients can access their own medical history");
        }

        Patient patient = patientService.getProfileByAuthId(authId);
        MedicalHistory history = medicalHistoryService.getMedicalHistoryByPatientId(patient.getPatientId());
        return ResponseEntity.ok(history);
    }

    /**
     * GET /patients/{patientId}/medical-history/records
     * Retrieve only the array of medical records for a patient
     */
    @GetMapping("/{patientId}/medical-history/records")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecords(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);

        // Doctors, admins can view any patient's records
        // Patients can only view their own
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }

        List<MedicalRecord> records = medicalHistoryService.getMedicalRecordsByPatientId(patientId);
        return ResponseEntity.ok(records);
    }

    /**
     * GET /patients/me/medical-history/records
     * Retrieve medical records for the current authenticated patient
     */
    @GetMapping("/me/medical-history/records")
    public ResponseEntity<List<MedicalRecord>> getCurrentMedicalRecords(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Only patients can access their own medical records");
        }

        Patient patient = patientService.getProfileByAuthId(authId);
        List<MedicalRecord> records = medicalHistoryService.getMedicalRecordsByPatientId(patient.getPatientId());
        return ResponseEntity.ok(records);
    }

    /**
     * GET /patients/{patientId}/medical-history/count
     * Retrieve the count of medical records for a patient
     */
    @GetMapping("/{patientId}/medical-history/count")
    public ResponseEntity<Long> getMedicalRecordCount(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);

        // Doctors, admins can view any patient's record count
        // Patients can only view their own
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }

        long count = medicalHistoryService.getRecordCountByPatientId(patientId);
        return ResponseEntity.ok(count);
    }

    /**
     * GET /patients/me/medical-history/count
     * Retrieve record count for the current authenticated patient
     */
    @GetMapping("/me/medical-history/count")
    public ResponseEntity<Long> getCurrentMedicalRecordCount(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Only patients can access their own medical record count");
        }

        Patient patient = patientService.getProfileByAuthId(authId);
        long count = medicalHistoryService.getRecordCountByPatientId(patient.getPatientId());
        return ResponseEntity.ok(count);
    }
}
