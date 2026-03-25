package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Patient> findByAuthId(String authId);
    Optional<Patient> findByPatientId(String patientId);
}