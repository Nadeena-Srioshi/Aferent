package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.ReferenceDataRequest;
import com.aferent.doctor_service.dto.SpecializationResponse;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.model.Hospital;
import com.aferent.doctor_service.model.Specialization;
import com.aferent.doctor_service.repository.HospitalRepository;
import com.aferent.doctor_service.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataService {

    private final HospitalRepository hospitalRepository;
    private final SpecializationRepository specializationRepository;

    // ─── hospitals ───────────────────────────────────────────────────

    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findByActiveTrue();
    }

    public Hospital addHospital(ReferenceDataRequest request) {
        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .city(request.getCity())
                .active(true)
                .build();
        Hospital saved = hospitalRepository.save(hospital);
        log.info("Hospital added: {}", saved.getName());
        return saved;
    }

    public Hospital updateHospital(String id, ReferenceDataRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found: " + id));
        hospital.setName(request.getName());
        hospital.setCity(request.getCity());
        hospital.setActive(request.isActive());
        return hospitalRepository.save(hospital);
    }

    public void deleteHospital(String id) {
        if (!hospitalRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hospital not found: " + id);
        }
        hospitalRepository.deleteById(id);
        log.info("Hospital deleted: {}", id);
    }

    // ─── specializations ─────────────────────────────────────────────

    public List<SpecializationResponse> getAllSpecializations(boolean includeFees) {
        return specializationRepository.findByActiveTrue()
                .stream()
                .map(spec -> SpecializationResponse.builder()
                        .id(spec.getId())
                        .name(spec.getName())
                        .maxVideoConsultationFee(includeFees ? spec.getMaxVideoConsultationFee() : null)
                        .maxPhysicalConsultationFee(includeFees ? spec.getMaxPhysicalConsultationFee() : null)
                .createdAt(spec.getCreatedAt())
                .updatedAt(spec.getUpdatedAt())
                        .build())
                .toList();
    }

    public Specialization addSpecialization(ReferenceDataRequest request) {
        Specialization spec = Specialization.builder()
                .name(request.getName())
                .maxVideoConsultationFee(request.getMaxVideoConsultationFee())
                .maxPhysicalConsultationFee(request.getMaxPhysicalConsultationFee())
                .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
                .build();
        Specialization saved = specializationRepository.save(spec);
        log.info("Specialization added: {}", saved.getName());
        return saved;
    }

    public Specialization updateSpecialization(String id, ReferenceDataRequest request) {
        Specialization spec = specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialization not found: " + id));
        spec.setName(request.getName());
        spec.setMaxVideoConsultationFee(request.getMaxVideoConsultationFee());
        spec.setMaxPhysicalConsultationFee(request.getMaxPhysicalConsultationFee());
        spec.setActive(request.isActive());
        spec.setUpdatedAt(LocalDateTime.now());
        return specializationRepository.save(spec);
    }

    public void deleteSpecialization(String id) {
        if (!specializationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Specialization not found: " + id);
        }
        specializationRepository.deleteById(id);
        log.info("Specialization deleted: {}", id);
    }
}