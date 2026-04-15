package com.aferent.doctor_service.repository;

import com.aferent.doctor_service.model.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    Optional<Prescription> findByPrescriptionId(String prescriptionId);
    List<Prescription> findByDoctorIdOrderByIssuedAtDesc(String doctorId);
    List<Prescription> findByDoctorIdAndPatientIdOrderByIssuedAtDesc(String doctorId, String patientId);

    // distinct patients for a doctor — get all prescriptions then deduplicate in service
    List<Prescription> findByDoctorId(String doctorId);

    long countByDoctorIdAndPatientId(String doctorId, String patientId);
}