package com.aferent.appointment_service.repository;

import com.aferent.appointment_service.model.AppointmentType;
import com.aferent.appointment_service.model.GeneratedSlot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedSlotRepository extends MongoRepository<GeneratedSlot, String> {

    // Find slots by doctor
    List<GeneratedSlot> findByDoctorId(String doctorId);
    
    // Find slots by doctor and date
    List<GeneratedSlot> findByDoctorIdAndDate(String doctorId, LocalDate date);

    List<GeneratedSlot> findByDoctorIdAndTypeAndDateGreaterThanEqualAndBookedFalse(
            String doctorId, AppointmentType type, LocalDate from);

    List<GeneratedSlot> findByScheduleIdAndDate(String scheduleId, LocalDate date);

    List<GeneratedSlot> findByScheduleIdAndDateAndBookedFalse(
            String scheduleId, LocalDate date);

    Optional<GeneratedSlot> findByScheduleIdAndDateAndVideoSlotId(
            String scheduleId, LocalDate date, String videoSlotId);

    List<GeneratedSlot> findByScheduleIdAndBookedFalse(String scheduleId);

    // Used when friend updates/deletes a schedule — clean up future unbooked slots
    void deleteByScheduleIdAndDateGreaterThanEqualAndBookedFalse(
            String scheduleId, LocalDate from);

    void deleteByDoctorIdAndDateAndBookedFalse(String doctorId, LocalDate date);

    void deleteByScheduleIdAndDateAndBookedFalse(String scheduleId, LocalDate date);

    int countByScheduleIdAndDateAndBookedTrue(String scheduleId, LocalDate date);
}