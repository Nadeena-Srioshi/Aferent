package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.MedicalHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalHistoryRepository extends MongoRepository<MedicalHistory, String> {

    /**
     * Find medical history by patient ID
     */
    Optional<MedicalHistory> findByPatientId(String patientId);
}
