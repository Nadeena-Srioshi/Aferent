package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.model.UploadStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<PatientDocument, String> {
    // find all non-deleted documents for a patient
    List<PatientDocument> findByPatientIdAndDeletedFalse(String patientId);

    List<PatientDocument> findByPatientIdAndDocumentTypeAndDeletedFalse(String patientId, String documentType);

    List<PatientDocument> findByPatientIdAndDocumentTypeInAndDeletedFalse(String patientId, List<String> documentTypes);

    List<PatientDocument> findByPatientIdAndIdInAndDeletedFalse(String patientId, List<String> ids);

    List<PatientDocument> findByPatientIdAndIdInAndDocumentTypeAndDeletedFalse(String patientId, List<String> ids, String documentType);

    List<PatientDocument> findByMinioKey(String minioKey);

    List<PatientDocument> findByMinioKeyAndUploadStatus(String minioKey, UploadStatus uploadStatus);

    List<PatientDocument> findByUploadStatusAndExpiresAtBefore(UploadStatus uploadStatus, LocalDateTime threshold);
}
