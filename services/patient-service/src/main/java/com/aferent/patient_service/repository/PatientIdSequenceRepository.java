package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.PatientIdSequence;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientIdSequenceRepository extends MongoRepository<PatientIdSequence, String> {
}
