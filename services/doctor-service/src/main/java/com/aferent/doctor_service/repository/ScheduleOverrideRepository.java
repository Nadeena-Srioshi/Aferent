package com.aferent.doctor_service.repository;

import com.aferent.doctor_service.model.ScheduleOverride;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleOverrideRepository extends MongoRepository<ScheduleOverride, String> {
    List<ScheduleOverride> findByDoctorId(String doctorId);
    List<ScheduleOverride> findByDoctorIdAndDateGreaterThanEqual(String doctorId, LocalDate date);
    Optional<ScheduleOverride> findByDoctorIdAndDate(String doctorId, LocalDate date);
}