package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.PatientVisitSummary;
import com.aferent.doctor_service.dto.PrescriptionPublicView;
import com.aferent.doctor_service.dto.PrescriptionRequest;
import com.aferent.doctor_service.model.Prescription;
import com.aferent.doctor_service.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // issue prescription — doctor only
    @PostMapping("/doctors/{doctorId}/prescriptions")
    public ResponseEntity<Map<String, Object>> issuePrescription(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @Valid @RequestBody PrescriptionRequest request
    ) {
        Prescription saved = prescriptionService.issuePrescription(doctorId, authId, request);
        return ResponseEntity.ok(Map.of(
                "prescription", saved,
                "message", "Prescription issued successfully. Patient notified via SMS and email."
        ));
    }

    // get all prescriptions issued by this doctor
    @GetMapping("/doctors/{doctorId}/prescriptions")
    public ResponseEntity<List<Prescription>> getDoctorPrescriptions(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId
    ) {
        return ResponseEntity.ok(
                prescriptionService.getDoctorPrescriptions(doctorId, authId));
    }

    // get all patients of this doctor
    @GetMapping("/doctors/{doctorId}/patients")
    public ResponseEntity<List<PatientVisitSummary>> getDoctorPatients(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId
    ) {
        return ResponseEntity.ok(
                prescriptionService.getDoctorPatients(doctorId, authId));
    }

    // get all prescriptions for a specific patient from this doctor
    @GetMapping("/doctors/{doctorId}/patients/{patientId}/prescriptions")
    public ResponseEntity<List<Prescription>> getPatientPrescriptions(
            @PathVariable String doctorId,
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId
    ) {
        return ResponseEntity.ok(
                prescriptionService.getPatientPrescriptions(doctorId, authId, patientId));
    }

    // public endpoint — what patient sees after scanning QR
    @GetMapping("/prescriptions/{prescriptionId}/view")
    public ResponseEntity<PrescriptionPublicView> getPublicView(
            @PathVariable String prescriptionId
    ) {
        return ResponseEntity.ok(prescriptionService.getPublicView(prescriptionId));
    }
}