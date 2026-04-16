package com.aferent.patient_service.service;

import com.aferent.patient_service.dto.DocumentMetadataRequest;
import com.aferent.patient_service.dto.MedicalReportSummaryResponse;
import com.aferent.patient_service.dto.PresignedUrlResponse;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.exception.ResourceNotFoundException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.model.PatientDocumentType;
import com.aferent.patient_service.model.UploadStatus;
import com.aferent.patient_service.repository.DocumentRepository;
import com.aferent.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientDocumentService {

    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;
    private final DocumentServiceGateway documentServiceGateway;
    private final PatientDoctorAccessService patientDoctorAccessService;

    @Value("${app.uploads.pending-retention-hours:24}")
    private int pendingRetentionHours;

    @Value("${app.uploads.bucket:app-storage}")
    private String storageBucket;

        private static final List<String> MEDICAL_REPORT_TYPES = List.of(
            PatientDocumentType.MEDICAL_REPORT.name(),
            PatientDocumentType.PRESCRIPTION.name(),
            PatientDocumentType.SCAN.name()
        );

    public PresignedUrlResponse generateUploadUrlForCurrentUser(String authId, String fileName, String contentType,
                                PatientDocumentType type, String displayName) {
    Patient patient = patientRepository.findByAuthId(authId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

    return generateUploadUrlInternal(patient, fileName, contentType, type, displayName);
    }

    public PresignedUrlResponse generateUploadUrl(String patientId, String fileName, String contentType, PatientDocumentType type) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

    return generateUploadUrlInternal(patient, fileName, contentType, type, null);
    }

    private PresignedUrlResponse generateUploadUrlInternal(Patient patient, String fileName, String contentType,
                               PatientDocumentType type, String displayName) {
    String safeFileName = (fileName == null || fileName.isBlank()) ? "file" : fileName;
    String safeContentType = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;

        String documentId = UUID.randomUUID().toString();
        String category = type.categoryPrefix() + "/" + patient.getPatientId();

        DocumentServiceGateway.PresignUploadResult presignResult = documentServiceGateway.generateUploadUrl(
                type.visibility(),
                category,
        safeFileName,
                type.appendUuid()
        );

    String objectKey = presignResult.objectKey();
    if (objectKey == null || objectKey.isBlank()) {
        objectKey = deriveObjectKeyFromUrl(presignResult.permanentUrl() != null ? presignResult.permanentUrl() : presignResult.uploadUrl());
    }

    LocalDateTime now = LocalDateTime.now();
    PatientDocument pending = PatientDocument.builder()
        .id(documentId)
        .patientId(patient.getPatientId())
        .fileName(safeFileName)
        .originalFileName(safeFileName)
        .displayName((displayName == null || displayName.isBlank()) ? safeFileName : displayName)
        .contentType(safeContentType)
        .minioKey(objectKey)
        .permanentUrl(presignResult.permanentUrl())
        .visibility(type.visibility())
        .category(category)
        .documentType(type.name())
        .uploadStatus(UploadStatus.PENDING_UPLOAD)
        .requestedAt(now)
        .expiresAt(now.plusHours(pendingRetentionHours))
        .build();

    documentRepository.save(pending);

        return PresignedUrlResponse.builder()
                .uploadUrl(presignResult.uploadUrl())
                .permanentUrl(presignResult.permanentUrl())
                .build();
    }

    public PatientDocument saveDocumentMetadata(String patientId, DocumentMetadataRequest req) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        PatientDocumentType type = PatientDocumentType.fromString(req.getDocumentType());
        String visibility = req.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            visibility = type.visibility();
        }

        String category = req.getCategory();
        if (category == null || category.isBlank()) {
            category = type.categoryPrefix() + "/" + patientId;
        }

        String minioKey = req.getMinioKey();
        if (minioKey == null || minioKey.isBlank()) {
            String fileName = req.getOriginalFileName() == null ? "file" : req.getOriginalFileName();
            minioKey = "patient-service/" + visibility + "/" + category + "/" + fileName;
        }

        PatientDocument existing = documentRepository.findById(req.getDocumentId()).orElse(null);
        if (existing != null) {
            existing.setOriginalFileName(req.getOriginalFileName());
            existing.setFileName(req.getOriginalFileName());
            existing.setDisplayName((req.getDisplayName() == null || req.getDisplayName().isBlank())
                    ? existing.getDisplayName()
                    : req.getDisplayName());
            existing.setContentType(req.getContentType());
            existing.setFileSize(req.getFileSize());
            existing.setMinioKey(minioKey);
            if (req.getPermanentUrl() != null && !req.getPermanentUrl().isBlank()) {
                existing.setPermanentUrl(req.getPermanentUrl());
            }
            existing.setVisibility(visibility);
            existing.setCategory(category);
            existing.setDocumentType(type.name());
            existing.setUploadStatus(UploadStatus.UPLOADED);
            if (existing.getUploadedAt() == null) {
                existing.setUploadedAt(LocalDateTime.now());
            }
            return documentRepository.save(existing);
        }

        return documentRepository.save(PatientDocument.builder()
            .id(req.getDocumentId())
            .patientId(patientId)
            .fileName(req.getOriginalFileName())
            .originalFileName(req.getOriginalFileName())
            .displayName((req.getDisplayName() == null || req.getDisplayName().isBlank()) ? req.getOriginalFileName() : req.getDisplayName())
            .contentType(req.getContentType())
            .fileSize(req.getFileSize())
            .minioKey(minioKey)
            .permanentUrl(req.getPermanentUrl())
            .visibility(visibility)
            .category(category)
            .documentType(type.name())
            .uploadStatus(UploadStatus.UPLOADED)
            .requestedAt(LocalDateTime.now())
            .uploadedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(pendingRetentionHours))
            .build());
    }

    public List<PatientDocument> getDocuments(String patientId, String documentType) {
        return getDocuments(patientId, documentType, "ADMIN", null);
    }

    public List<PatientDocument> getDocuments(String patientId, String documentType, String role, String requesterAuthId) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        boolean hasTypeFilter = documentType != null && !documentType.isBlank();

        // Patient and Admin: full non-deleted view (no access filtering by document expiresAt)
        if (!"DOCTOR".equalsIgnoreCase(role)) {
            if (!hasTypeFilter) {
                return documentRepository.findByPatientIdAndDeletedFalse(patientId);
            }
            return documentRepository.findByPatientIdAndDocumentTypeAndDeletedFalse(patientId, documentType);
        }

        if (requesterAuthId == null || requesterAuthId.isBlank()) {
            throw new ForbiddenOperationException("Doctor identity is required");
        }

        Set<String> allowedIds = patientDoctorAccessService.getAllowedDocumentIds(patientId, requesterAuthId);
        if (allowedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> allowedIdList = new ArrayList<>(allowedIds);
        if (!hasTypeFilter) {
            return documentRepository.findByPatientIdAndIdInAndDeletedFalse(patientId, allowedIdList);
        }
        return documentRepository.findByPatientIdAndIdInAndDocumentTypeAndDeletedFalse(patientId, allowedIdList, documentType);
    }

    public String getDocumentDownloadUrl(String patientId, String documentId) {
        return getDocumentDownloadUrl(patientId, documentId, "ADMIN", null);
    }

    public String getDocumentDownloadUrl(String patientId, String documentId, String role, String requesterAuthId) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        PatientDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!doc.getPatientId().equals(patientId)) {
            throw new ForbiddenOperationException("Access denied");
        }

        if ("DOCTOR".equalsIgnoreCase(role)) {
            if (requesterAuthId == null || requesterAuthId.isBlank()) {
                throw new ForbiddenOperationException("Doctor identity is required");
            }

            boolean hasAccess = patientDoctorAccessService.canDoctorAccessDocument(patientId, requesterAuthId, documentId);
            if (!hasAccess) {
                throw new ForbiddenOperationException("Doctor does not have permission to access this document");
            }
        }

        if (doc.getUploadStatus() != UploadStatus.UPLOADED) {
            throw new ForbiddenOperationException("Document is not yet available for viewing");
        }

        if ("public".equalsIgnoreCase(doc.getVisibility())
                && doc.getPermanentUrl() != null
                && !doc.getPermanentUrl().isBlank()) {
            return doc.getPermanentUrl();
        }

        return documentServiceGateway.generateDownloadUrl(doc.getMinioKey(), 3600);
    }

    public String getDocumentDownloadUrlForCurrentUser(String authId, String documentId) {
        Patient patient = patientRepository.findByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));
        return getDocumentDownloadUrl(patient.getPatientId(), documentId, "PATIENT", authId);
        }

        public List<MedicalReportSummaryResponse> getMedicalReportsSummary(String patientId) {
        patientRepository.findByPatientId(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        return documentRepository.findByPatientIdAndDocumentTypeInAndDeletedFalse(patientId, MEDICAL_REPORT_TYPES)
            .stream()
            .filter(doc -> doc.getUploadStatus() == UploadStatus.UPLOADED)
            .map(doc -> MedicalReportSummaryResponse.builder()
                .documentId(doc.getId())
                .originalFileName(doc.getOriginalFileName())
                .displayName(doc.getDisplayName())
                .uploadedAt(doc.getUploadedAt())
                .contentType(doc.getContentType())
                .documentType(doc.getDocumentType())
                .build())
            .toList();
    }

    public void deleteDocument(String patientId, String documentId) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        PatientDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!doc.getPatientId().equals(patientId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        doc.setDeleted(true);
        documentRepository.save(doc);
    }

    public void markUploadedByObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        List<String> candidates = Arrays.asList(
                objectKey,
                stripBucketPrefix(objectKey)
        );

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            List<PatientDocument> pendingDocs = documentRepository.findByMinioKeyAndUploadStatus(candidate, UploadStatus.PENDING_UPLOAD);
            if (pendingDocs.isEmpty()) {
                List<PatientDocument> sameKeyDocs = documentRepository.findByMinioKey(candidate);
                List<PatientDocument> toUpdate = sameKeyDocs.stream()
                        .filter(doc -> doc.getUploadStatus() != UploadStatus.UPLOADED)
                        .toList();
                if (!toUpdate.isEmpty()) {
                    LocalDateTime uploadedAt = LocalDateTime.now();
                    for (PatientDocument doc : toUpdate) {
                        doc.setUploadStatus(UploadStatus.UPLOADED);
                        if (doc.getUploadedAt() == null) {
                            doc.setUploadedAt(uploadedAt);
                        }
                    }
                    documentRepository.saveAll(toUpdate);
                    log.info("Marked {} existing upload record(s) completed for key={}", toUpdate.size(), candidate);
                    return;
                }
                continue;
            }

            LocalDateTime uploadedAt = LocalDateTime.now();
            for (PatientDocument doc : pendingDocs) {
                doc.setUploadStatus(UploadStatus.UPLOADED);
                doc.setUploadedAt(uploadedAt);
            }
            documentRepository.saveAll(pendingDocs);
            log.info("Marked {} pending upload(s) completed for key={}", pendingDocs.size(), candidate);
            return;
        }

        log.warn("Received upload event but found no pending record for key={}", objectKey);
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void expirePendingUploads() {
        LocalDateTime now = LocalDateTime.now();
        List<PatientDocument> stale = documentRepository.findByUploadStatusAndExpiresAtBefore(UploadStatus.PENDING_UPLOAD, now);
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(doc -> doc.setUploadStatus(UploadStatus.EXPIRED));
        documentRepository.saveAll(stale);
        log.info("Expired {} pending uploads older than retention threshold", stale.size());
    }

    private String deriveObjectKeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            return stripBucketPrefix(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stripBucketPrefix(String rawKey) {
        if (rawKey == null) {
            return null;
        }

        String prefix = storageBucket + "/";
        if (rawKey.startsWith(prefix) && rawKey.length() > prefix.length()) {
            return rawKey.substring(prefix.length());
        }

        if (rawKey.startsWith("/" + prefix) && rawKey.length() > prefix.length() + 1) {
            return rawKey.substring(prefix.length() + 1);
        }

        if (rawKey.isBlank()) {
            return rawKey;
        }
        return rawKey;
    }
}
