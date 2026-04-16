package com.aferent.appointment_service.repository;

import com.aferent.appointment_service.model.AppointmentStatus;
import com.aferent.appointment_service.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByDoctorId(String doctorId);

    List<Appointment> findByDoctorIdAndStatus(String doctorId, AppointmentStatus status);

    List<Appointment> findByDoctorAuthId(String doctorAuthId);

    List<Appointment> findByDoctorAuthIdAndStatus(String doctorAuthId, AppointmentStatus status);

    List<Appointment> findAllByOrderByCreatedAtDesc();
}