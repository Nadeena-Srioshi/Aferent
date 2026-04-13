package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.PatientDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<PatientDocument, String> {
    // find all non-deleted documents for a patient
    List<PatientDocument> findByPatientIdAndDeletedFalse(String patientId);
}