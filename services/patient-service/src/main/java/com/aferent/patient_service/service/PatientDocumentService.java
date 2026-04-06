package com.aferent.patient_service.service;

import com.aferent.patient_service.dto.DocumentMetadataRequest;
import com.aferent.patient_service.dto.PresignedUrlResponse;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.exception.ResourceNotFoundException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.model.PatientDocumentType;
import com.aferent.patient_service.repository.DocumentRepository;
import com.aferent.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientDocumentService {

    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;
    private final DocumentServiceGateway documentServiceGateway;

    public PresignedUrlResponse generateUploadUrl(String patientId, String fileName, String contentType, PatientDocumentType type) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        String documentId = UUID.randomUUID().toString();
        String category = type.categoryPrefix() + "/" + patient.getPatientId();

        DocumentServiceGateway.PresignUploadResult presignResult = documentServiceGateway.generateUploadUrl(
                type.visibility(),
                category,
                fileName,
                type.appendUuid()
        );

        return PresignedUrlResponse.builder()
                .uploadUrl(presignResult.uploadUrl())
                .documentId(documentId)
                .minioKey(presignResult.objectKey())
                .permanentUrl(presignResult.permanentUrl())
                .visibility(type.visibility())
                .category(category)
                .expirySeconds(900)
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

        return documentRepository.save(PatientDocument.builder()
                .id(req.getDocumentId())
                .patientId(patientId)
                .fileName(req.getOriginalFileName())
                .originalFileName(req.getOriginalFileName())
                .contentType(req.getContentType())
                .fileSize(req.getFileSize())
                .minioKey(minioKey)
                .visibility(visibility)
                .category(category)
                .documentType(type.name())
                .build());
    }

    public List<PatientDocument> getDocuments(String patientId, String documentType) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (documentType == null || documentType.isBlank()) {
            return documentRepository.findByPatientIdAndDeletedFalse(patientId);
        }
        return documentRepository.findByPatientIdAndDocumentTypeAndDeletedFalse(patientId, documentType);
    }

    public String getDocumentDownloadUrl(String patientId, String documentId) {
        patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        PatientDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!doc.getPatientId().equals(patientId)) {
            throw new ForbiddenOperationException("Access denied");
        }
        return documentServiceGateway.generateDownloadUrl(doc.getMinioKey(), 3600);
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
}
