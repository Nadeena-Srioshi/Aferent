package com.aferent.patient_service.service;

import com.aferent.patient_service.dto.*;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientIdSequence;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.repository.DocumentRepository;
import com.aferent.patient_service.repository.PatientRepository;
import com.aferent.patient_service.repository.PatientIdSequenceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;
    private final PatientIdSequenceRepository patientIdSequenceRepository;
    private final MinioService minioService;

    @PostConstruct   // runs once when service starts up
    public void init() {
        minioService.ensureBucketExists();
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
                .orElseThrow(() -> new RuntimeException("Patient ID sequence not initialized"));
        sequence.setSequence(sequence.getSequence() + 1);
        PatientIdSequence updated = patientIdSequenceRepository.save(sequence);
        return String.format("PAT_%03d", updated.getSequence());  // e.g., PAT_001, PAT_002
    }

    // called by Kafka consumer when user.registered event arrives
    // creates an empty patient profile linked to the auth authId
    public Patient createProfile(String authId, String email) {
        if (patientRepository.existsByEmail(email)) {
            log.info("Profile already exists for email: {}", email);
            return patientRepository.findByEmail(email).orElseThrow();
        }
        
        String patientId = generatePatientId();
        Patient patient = Patient.builder()
                .authId(authId)          // auth-service user ID
                .patientId(patientId)    // human-readable ID (PAT_001, etc.)
                .email(email)
                .build();
        log.info("Creating patient profile: authId={}, patientId={}, email={}", authId, patientId, email);
        return patientRepository.save(patient);
    }

    public Patient getProfile(String patientId) {
        return patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient updateProfile(String patientId, String requesterId, UpdateProfileRequest req) {
        // requesterId is the authId from X-User-ID header
        // security check — patients can only update their own profile
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        if (!patient.getAuthId().equals(requesterId)) {
            throw new RuntimeException("Access denied");
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

    // Step 1 of document upload — generate presigned URL
    // client uses this URL to upload directly to MinIO
    public PresignedUrlResponse generateUploadUrl(String patientId, String fileName, String contentType) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        String documentId = UUID.randomUUID().toString();
        // minioKey = folder structure inside the bucket
        String minioKey = "patients/" + patientId + "/" + documentId + "/" + fileName;

        String uploadUrl = minioService.generateUploadUrl(minioKey);

        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .documentId(documentId)
                .minioKey(minioKey)
                .expirySeconds(900)   // 15 minutes
                .build();
    }

    // Step 2 of document upload — client calls this AFTER uploading to MinIO
    // saves the metadata so we can find the file later
    public PatientDocument saveDocumentMetadata(String patientId, DocumentMetadataRequest req) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        PatientDocument doc = PatientDocument.builder()
                .id(req.getDocumentId())
                .patientId(patientId)
                .originalFileName(req.getOriginalFileName())
                .contentType(req.getContentType())
                .fileSize(req.getFileSize())
                .documentType(req.getDocumentType())
                .minioKey("patients/" + patientId + "/" + req.getDocumentId() + "/" + req.getOriginalFileName())
                .build();
        return documentRepository.save(doc);
    }

    // returns documents with fresh presigned download URLs
    public List<PatientDocument> getDocuments(String patientId) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return documentRepository.findByPatientIdAndDeletedFalse(patientId);
    }

    public String getDocumentDownloadUrl(String patientId, String documentId) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        PatientDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getPatientId().equals(patientId)) {
            throw new RuntimeException("Access denied");
        }
        return minioService.generateDownloadUrl(doc.getMinioKey());
    }

    public void deleteDocument(String patientId, String documentId) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        PatientDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getPatientId().equals(patientId)) {
            throw new RuntimeException("Access denied");
        }
        doc.setDeleted(true);   // soft delete — keep record, hide from listing
        documentRepository.save(doc);
        minioService.deleteObject(doc.getMinioKey());
    }
}