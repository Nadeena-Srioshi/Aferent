// repository/GeneratedSlotRepository.java
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

    int countByScheduleIdAndDateAndBookedTrue(String scheduleId, LocalDate date);
}