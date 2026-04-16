package com.aferent.doctor_service.repository;

import com.aferent.doctor_service.model.WeeklySchedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WeeklyScheduleRepository extends MongoRepository<WeeklySchedule, String> {
    Optional<WeeklySchedule> findByDoctorId(String doctorId);
    boolean existsByDoctorId(String doctorId);
    void deleteByDoctorId(String doctorId);
}