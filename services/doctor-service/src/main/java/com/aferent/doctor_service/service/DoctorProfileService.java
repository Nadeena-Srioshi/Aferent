package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.UpdateProfileRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.Hospital;
import com.aferent.doctor_service.model.ScheduleOverride;
import com.aferent.doctor_service.model.Specialization;
import com.aferent.doctor_service.model.WeeklySchedule;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.HospitalRepository;
import com.aferent.doctor_service.repository.ScheduleOverrideRepository;
import com.aferent.doctor_service.repository.SpecializationRepository;
import com.aferent.doctor_service.repository.WeeklyScheduleRepository;
import com.aferent.doctor_service.service.DocumentServiceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorProfileService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final SpecializationRepository specializationRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final DocumentServiceGateway documentServiceGateway;

    public List<Doctor> getAllActiveDoctors() {
        return doctorRepository.findByStatus(Doctor.RegistrationStatus.ACTIVE);
    }

    public List<Doctor> searchActiveDoctors(String specializationId, String specialty, String name, String hospital, String date) {
        String normalizedSpecializationId = normalize(specializationId);
        String normalizedSpecialty = normalize(specialty);
        String normalizedName = normalize(name);
        String normalizedHospital = normalize(hospital);

        Set<String> specializationIds = resolveSpecializationIds(normalizedSpecializationId, normalizedSpecialty);

        LocalDate requestedDate = parseOptionalDate(date);
        Set<String> hospitalIds = resolveHospitalIds(normalizedHospital);

        return doctorRepository.findByStatus(Doctor.RegistrationStatus.ACTIVE)
                .stream()
            .filter(doctor -> specializationIds == null
                || (doctor.getSpecialization() != null && specializationIds.contains(doctor.getSpecialization())))
                .filter(doctor -> {
                    if (normalizedName == null) return true;
                    String fullName = ((doctor.getFirstName() == null ? "" : doctor.getFirstName()) + " "
                            + (doctor.getLastName() == null ? "" : doctor.getLastName())).trim();
                    return containsIgnoreCase(fullName, normalizedName)
                            || containsIgnoreCase(doctor.getFirstName(), normalizedName)
                            || containsIgnoreCase(doctor.getLastName(), normalizedName);
                })
                .filter(doctor -> {
                    if (normalizedHospital == null) return true;
                    List<String> assignedHospitals = doctor.getHospitals();
                    if (assignedHospitals == null || assignedHospitals.isEmpty()) return false;
                    if (!hospitalIds.isEmpty()) {
                        return assignedHospitals.stream().anyMatch(hospitalIds::contains);
                    }
                    return false;
                })
                .filter(doctor -> requestedDate == null
                        || hasAvailabilityOnDate(doctor.getDoctorId(), requestedDate, hospitalIds))
                .toList();
    }

    private Set<String> resolveSpecializationIds(String specializationId, String specialtyName) {
        if (specializationId != null) {
            return Set.of(specializationId);
        }

        if (specialtyName == null) {
            return null;
        }

        Set<String> resolved = specializationRepository.findByActiveTrue()
                .stream()
                .filter(spec -> containsIgnoreCase(spec.getName(), specialtyName))
                .map(Specialization::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        return resolved.isEmpty() ? Set.of("__NO_MATCH__") : resolved;
    }

    public Doctor getProfile(String doctorId) {
        return doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));
    }

    public List<Hospital> getMyHospitals(String authId) {
        Doctor doctor = doctorRepository.findByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found for authId=" + authId));

        List<String> assignedHospitalIds = doctor.getHospitals();
        if (assignedHospitalIds == null || assignedHospitalIds.isEmpty()) {
            return List.of();
        }

        Set<String> dedupedIds = new LinkedHashSet<>();
        for (String hospitalId : assignedHospitalIds) {
            if (hospitalId != null && !hospitalId.isBlank()) {
                dedupedIds.add(hospitalId);
            }
        }

        if (dedupedIds.isEmpty()) {
            return List.of();
        }

        Map<String, Hospital> activeHospitalsById = hospitalRepository.findAllById(dedupedIds)
                .stream()
                .filter(Hospital::isActive)
                .filter(hospital -> hospital.getId() != null)
                .collect(java.util.stream.Collectors.toMap(Hospital::getId, hospital -> hospital));

        return dedupedIds.stream()
                .map(activeHospitalsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public Doctor updateProfile(String doctorId, String authId,
                                UpdateProfileRequest request) {
        Doctor doctor = validateOwnershipAndActive(doctorId, authId);

        if (request.getFirstName() != null)
            doctor.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            doctor.setLastName(request.getLastName());

        if (request.getPhone() != null)
            doctor.setPhone(request.getPhone());

        if (request.getBio() != null)
            doctor.setBio(request.getBio());

        if (request.getLanguages() != null)
            doctor.setLanguages(request.getLanguages());

        if (request.getYearsOfExperience() != null)
            doctor.setYearsOfExperience(request.getYearsOfExperience());

        if (request.getQualifications() != null)
            doctor.setQualifications(request.getQualifications());

        if (request.getHospitalIds() != null) {
            // validate every hospitalId exists in master collection
            for (String hospitalId : request.getHospitalIds()) {
                if (!hospitalRepository.existsById(hospitalId)) {
                    throw new IllegalArgumentException(
                            "Invalid hospitalId: " + hospitalId
                            + ". Select from the hospitals list.");
                }
            }
            doctor.setHospitals(request.getHospitalIds());
        }

        Doctor saved = doctorRepository.save(doctor);
        log.info("Profile updated for doctorId={}", doctorId);
        return saved;
    }

    public Map<String, String> getProfilePicUploadUrl(String authId, String fileName) {
        Doctor doctor = doctorRepository.findByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found for authId=" + authId));

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException(
                    "Account not active. Current status: " + doctor.getStatus());
        }

        if (!isImageFile(fileName)) {
            throw new IllegalArgumentException(
                    "Only image files are allowed for profile picture");
        }

        String category = "profile-picture/" + doctor.getDoctorId();

        // Get presigned URL from document-service
        DocumentServiceGateway.PresignUploadResult result = documentServiceGateway.generateUploadUrl(
                "public",
                category,
                fileName,
                true  // append UUID to avoid collisions
        );

        // Store the permanent URL immediately (for public files, this URL is stable)
        if (result.permanentUrl() != null && !result.permanentUrl().isBlank()) {
            doctor.setProfilePicUrl(result.permanentUrl());
            doctorRepository.save(doctor);
            log.info("Profile picture URL stored proactively for doctorId={}", doctor.getDoctorId());
        }

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", result.uploadUrl());
        if (result.permanentUrl() != null) {
            response.put("permanentUrl", result.permanentUrl());
        }
        return response;
    }

    public String getProfilePicUrl(String doctorId) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (doctor.getProfilePicUrl() == null || doctor.getProfilePicUrl().isBlank()) {
            throw new ResourceNotFoundException(
                    "No profile picture uploaded for doctorId=" + doctorId);
        }

        return doctor.getProfilePicUrl();
    }

    public String getLicenseSignedUrl(String doctorId, String requesterAuthId, String requesterRole, int expiresSeconds) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
            if (!"DOCTOR".equalsIgnoreCase(requesterRole)) {
                throw new ForbiddenOperationException("Only the doctor owner or an admin can access license documents");
            }
            if (!doctor.getAuthId().equals(requesterAuthId)) {
                throw new ForbiddenOperationException("You can only access your own license document");
            }
        }

        if (doctor.getLicenseDocKey() == null || doctor.getLicenseDocKey().isBlank()) {
            throw new ResourceNotFoundException(
                    "No license document uploaded for doctorId=" + doctorId);
        }

        if (expiresSeconds <= 0 || expiresSeconds > 604800) {
            throw new IllegalArgumentException("expires must be between 1 and 604800 seconds");
        }

        return documentServiceGateway.generateDownloadUrl(doctor.getLicenseDocKey(), expiresSeconds);
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") 
                || lowerCase.endsWith(".png") || lowerCase.endsWith(".gif") 
                || lowerCase.endsWith(".webp");
    }

    private Doctor validateOwnershipAndActive(String doctorId, String authId) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (!doctor.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException(
                    "You can only manage your own profile");
        }

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException(
                    "Account not active. Current status: " + doctor.getStatus());
        }

        return doctor;
    }

    private LocalDate parseOptionalDate(String date) {
        String normalizedDate = normalize(date);
        if (normalizedDate == null) return null;

        try {
            return LocalDate.parse(normalizedDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date must be in YYYY-MM-DD format");
        }
    }

    private Set<String> resolveHospitalIds(String hospitalQuery) {
        if (hospitalQuery == null) return Set.of();

        List<Hospital> hospitals = hospitalRepository.findByActiveTrue();
        Set<String> resolved = new HashSet<>();

        for (Hospital h : hospitals) {
            if (h == null || h.getId() == null) continue;

            if (hospitalQuery.equalsIgnoreCase(h.getId())) {
                resolved.add(h.getId());
                continue;
            }

            String name = h.getName();
            if (name != null && name.toLowerCase().contains(hospitalQuery.toLowerCase())) {
                resolved.add(h.getId());
            }
        }

        return resolved;
    }

    private boolean hasAvailabilityOnDate(String doctorId, LocalDate date, Set<String> hospitalIds) {
        WeeklySchedule weeklySchedule = weeklyScheduleRepository.findByDoctorId(doctorId).orElse(null);
        if (weeklySchedule == null) {
            return false;
        }

        List<WeeklySchedule.DaySchedule> baseSlots = getSlotsByDay(weeklySchedule, date);
        if (baseSlots == null) {
            baseSlots = List.of();
        }

        ScheduleOverride override = scheduleOverrideRepository.findByDoctorIdAndDate(doctorId, date).orElse(null);
        if (override != null) {
            switch (override.getAction()) {
                case BLOCK:
                    return false;
                case ADD:
                    return hasMatchingHospitalSlot(override.getSlots(), hospitalIds);
                case CANCEL_SESSION:
                    String cancelledSessionId = override.getSessionId();
                    List<WeeklySchedule.DaySchedule> remaining = baseSlots.stream()
                            .filter(slot -> slot != null && (cancelledSessionId == null
                                    || !cancelledSessionId.equals(slot.getSessionId())))
                            .toList();
                    return hasMatchingHospitalSlot(remaining, hospitalIds);
                default:
                    return hasMatchingHospitalSlot(baseSlots, hospitalIds);
            }
        }

        return hasMatchingHospitalSlot(baseSlots, hospitalIds);
    }

    private List<WeeklySchedule.DaySchedule> getSlotsByDay(WeeklySchedule schedule, LocalDate date) {
        if (schedule == null || date == null) return List.of();

        return switch (date.getDayOfWeek()) {
            case MONDAY -> schedule.getMonday();
            case TUESDAY -> schedule.getTuesday();
            case WEDNESDAY -> schedule.getWednesday();
            case THURSDAY -> schedule.getThursday();
            case FRIDAY -> schedule.getFriday();
            case SATURDAY -> schedule.getSaturday();
            case SUNDAY -> schedule.getSunday();
        };
    }

    private boolean hasMatchingHospitalSlot(List<WeeklySchedule.DaySchedule> slots, Set<String> hospitalIds) {
        if (slots == null || slots.isEmpty()) return false;
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            return slots.stream().anyMatch(slot -> slot != null);
        }

        return slots.stream().anyMatch(slot -> slot != null
                && slot.getHospital() != null
                && hospitalIds.contains(slot.getHospital()));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) return false;
        return value.toLowerCase().contains(query.toLowerCase());
    }
}