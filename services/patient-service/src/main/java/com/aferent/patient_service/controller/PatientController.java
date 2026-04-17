package com.aferent.patient_service.controller;

import com.aferent.patient_service.dto.*;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientDoctorAccess;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.model.PatientDocumentType;
import com.aferent.patient_service.service.PatientDoctorAccessService;
import com.aferent.patient_service.service.PatientService;
import com.aferent.patient_service.service.PatientDocumentService;
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
    private final PatientDocumentService patientDocumentService;
    private final PatientDoctorAccessService patientDoctorAccessService;

    // gateway injects X-User-ID header — contains the authId from auth-service
    // {id} in URL is the patientId (e.g., PAT_001)

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients(
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenOperationException("Access denied");
        }

        return ResponseEntity.ok(patientService.getAllPatients());
    }

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

    @GetMapping("/me")
    public ResponseEntity<Patient> getCurrentProfile(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Access denied");
        }

        return ResponseEntity.ok(patientService.getProfileByAuthId(authId));
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
            @RequestParam String contentType,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String documentSubType
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (!patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(
            patientDocumentService.generateUploadUrl(patientId, fileName, contentType, PatientDocumentType.fromString(documentType), documentSubType)
        );
    }

        // Preferred frontend route — derives patient identity from JWT (X-User-ID)
        @PostMapping("/me/documents/upload-url")
        public ResponseEntity<PresignedUrlResponse> getUploadUrlForCurrentUser(
            @RequestHeader("X-User-ID") String authId,
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String documentSubType
        ) {
        return ResponseEntity.ok(
            patientDocumentService.generateUploadUrlForCurrentUser(
                authId,
                fileName,
                contentType,
                PatientDocumentType.fromString(documentType),
                displayName,
                documentSubType
            )
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
        return ResponseEntity.ok(patientDocumentService.saveDocumentMetadata(patientId, request));
    }

    // Optional fallback completion route for frontend recovery flows
    @PostMapping("/me/documents")
    public ResponseEntity<PatientDocument> saveDocumentForCurrentUser(
            @RequestHeader("X-User-ID") String authId,
            @RequestBody DocumentMetadataRequest request
    ) {
        String patientId = patientService.getProfileByAuthId(authId).getPatientId();
        return ResponseEntity.ok(patientDocumentService.saveDocumentMetadata(patientId, request));
    }

    // list documents — returns metadata only (no file content)
    @GetMapping("/{patientId}/documents")
    public ResponseEntity<List<PatientDocument>> getDocuments(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String documentType
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (role.equals("PATIENT") && !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(patientDocumentService.getDocuments(patientId, documentType, role, authId));
    }

    // lightweight medical reports listing for patients
    @GetMapping("/{patientId}/medical-reports")
    public ResponseEntity<List<MedicalReportSummaryResponse>> getMedicalReports(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        Patient patient = patientService.getProfile(patientId);
        if (!"PATIENT".equals(role) || !patient.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return ResponseEntity.ok(patientDocumentService.getMedicalReportsSummary(patientId));
    }

    @GetMapping("/me/documents")
    public ResponseEntity<List<PatientDocument>> getDocumentsForCurrentUser(
            @RequestHeader("X-User-ID") String authId,
            @RequestParam(required = false) String documentType
    ) {
        String patientId = patientService.getProfileByAuthId(authId).getPatientId();
        return ResponseEntity.ok(patientDocumentService.getDocuments(patientId, documentType, "PATIENT", authId));
    }

    @GetMapping("/me/medical-reports")
    public ResponseEntity<List<MedicalReportSummaryResponse>> getMedicalReportsForCurrentUser(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Access denied");
        }
        String patientId = patientService.getProfileByAuthId(authId).getPatientId();
        return ResponseEntity.ok(patientDocumentService.getMedicalReportsSummary(patientId));
    }

    @GetMapping("/me/documents/access")
    public ResponseEntity<List<DocumentAccessSummaryResponse>> getMyDocumentAccess(
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Access denied");
        }

        String patientId = patientService.getProfileByAuthId(authId).getPatientId();
        return ResponseEntity.ok(patientDoctorAccessService.getActiveAccessSummariesForPatient(patientId, authId));
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
        String url = patientDocumentService.getDocumentDownloadUrl(patientId, documentId, role, authId);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    @PostMapping("/{patientId}/documents/share")
    public ResponseEntity<PatientDoctorAccess> shareDocumentsWithDoctor(
            @PathVariable String patientId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ShareDocumentsRequest request
    ) {
        if (!"PATIENT".equals(role)) {
            throw new ForbiddenOperationException("Only patients can share documents");
        }

        PatientDoctorAccess result = patientDoctorAccessService.shareDocuments(patientId, authId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/documents/{documentId}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrlForCurrentUser(
            @PathVariable String documentId,
            @RequestHeader("X-User-ID") String authId
    ) {
        String url = patientDocumentService.getDocumentDownloadUrlForCurrentUser(authId, documentId);
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
        patientDocumentService.deleteDocument(patientId, documentId);
        return ResponseEntity.noContent().build();
    }
}