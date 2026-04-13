package com.aferent.patient_service.controller;

import com.aferent.patient_service.dto.*;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    // gateway injects X-User-ID header — contains the authId from auth-service
    // {id} in URL is the patientId (e.g., PAT_001)

    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getProfile(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);
        
        // doctors and admins can view any patient profile
        // patients can only view their own
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(patient);
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<Patient> updateProfile(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(patientService.updateProfile(patientId, authId, request));
    }

    // Step 1 — get a presigned URL to upload a document
    @PostMapping("/{patientId}/documents/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (!patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(
                patientService.generateUploadUrl(patientId, fileName, contentType)
        );
    }

    // Step 2 — confirm upload completed, save metadata
    @PostMapping("/{patientId}/documents")
    public ResponseEntity<PatientDocument> saveDocument(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestBody DocumentMetadataRequest request
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (!patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(patientService.saveDocumentMetadata(patientId, request));
    }

    // list documents — returns metadata only (no file content)
    @GetMapping("/{patientId}/documents")
    public ResponseEntity<List<PatientDocument>> getDocuments(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(patientService.getDocuments(patientId));
    }

    // get a presigned download URL for a specific document
    @GetMapping("/{patientId}/documents/{documentId}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable String patientId,
            @PathVariable String documentId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        String url = patientService.getDocumentDownloadUrl(patientId, documentId);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    @DeleteMapping("/{patientId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String patientId,
            @PathVariable String documentId,
            @RequestHeader("X-User-ID") String authId
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (!patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        patientService.deleteDocument(patientId, documentId);
        return ResponseEntity.noContent().build();
    }
}