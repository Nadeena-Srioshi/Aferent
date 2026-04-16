package com.aferent.patient_service.repository;

import com.aferent.patient_service.model.PatientDoctorAccess;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientDoctorAccessRepository extends MongoRepository<PatientDoctorAccess, String> {

    Optional<PatientDoctorAccess> findFirstByPatientIdAndDoctorAuthIdAndAppointmentIdAndActiveTrueOrderByGrantedAtDesc(
            String patientId,
            String doctorAuthId,
            String appointmentId
    );

    List<PatientDoctorAccess> findByPatientIdAndDoctorAuthIdAndActiveTrue(String patientId, String doctorAuthId);
}
