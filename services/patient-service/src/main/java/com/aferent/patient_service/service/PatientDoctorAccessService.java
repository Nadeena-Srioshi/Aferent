package com.aferent.patient_service.service;

import com.aferent.patient_service.dto.ShareDocumentsRequest;
import com.aferent.patient_service.exception.ForbiddenOperationException;
import com.aferent.patient_service.exception.ResourceNotFoundException;
import com.aferent.patient_service.model.Patient;
import com.aferent.patient_service.model.PatientDoctorAccess;
import com.aferent.patient_service.model.PatientDocument;
import com.aferent.patient_service.repository.DocumentRepository;
import com.aferent.patient_service.repository.PatientDoctorAccessRepository;
import com.aferent.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientDoctorAccessService {

    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;
    private final PatientDoctorAccessRepository patientDoctorAccessRepository;

    @Value("${app.doctor-access.default-days:7}")
    private int defaultAccessDays;

    public PatientDoctorAccess shareDocuments(String patientId, String requesterAuthId, ShareDocumentsRequest request) {
        Patient patient = patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (!patient.getAuthId().equals(requesterAuthId)) {
            throw new ForbiddenOperationException("Only the patient can share their documents");
        }

        List<PatientDocument> selectedDocuments = documentRepository.findAllById(request.getDocumentIds());
        if (selectedDocuments.size() != request.getDocumentIds().size()) {
            throw new ResourceNotFoundException("One or more selected documents were not found");
        }

        boolean invalidOwnership = selectedDocuments.stream().anyMatch(doc ->
                !patientId.equals(doc.getPatientId()) || doc.isDeleted());
        if (invalidOwnership) {
            throw new ForbiddenOperationException("You can share only your own non-deleted documents");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveExpiry = request.getExpiresAt() != null
                ? request.getExpiresAt()
                : now.plusDays(defaultAccessDays);

        if (!effectiveExpiry.isAfter(now)) {
            throw new ForbiddenOperationException("expiresAt must be in the future");
        }

        PatientDoctorAccess access = patientDoctorAccessRepository
                .findFirstByPatientIdAndDoctorAuthIdAndAppointmentIdAndActiveTrueOrderByGrantedAtDesc(
                        patientId,
                        request.getDoctorAuthId(),
                        request.getAppointmentId()
                )
                .orElseGet(() -> PatientDoctorAccess.builder()
                        .patientId(patientId)
                        .doctorId(request.getDoctorId())
                        .doctorAuthId(request.getDoctorAuthId())
                        .appointmentId(request.getAppointmentId())
                        .build());

        Set<String> deduplicated = new LinkedHashSet<>(request.getDocumentIds());
        access.setAllowedDocumentIds(deduplicated.stream().toList());
        access.setDoctorId(request.getDoctorId());
        access.setDoctorAuthId(request.getDoctorAuthId());
        access.setAppointmentId(request.getAppointmentId());
        access.setActive(true);
        access.setGrantedAt(now);
        access.setRevokedAt(null);
        access.setExpiresAt(effectiveExpiry);

        PatientDoctorAccess saved = patientDoctorAccessRepository.save(access);
        log.info("Updated doctor access patientId={} doctorAuthId={} appointmentId={} documents={} expiresAt={}",
                patientId,
                request.getDoctorAuthId(),
                request.getAppointmentId(),
                saved.getAllowedDocumentIds().size(),
                saved.getExpiresAt());

        return saved;
    }

    public Set<String> getAllowedDocumentIds(String patientId, String doctorAuthId) {
        LocalDateTime now = LocalDateTime.now();

        return patientDoctorAccessRepository
                .findByPatientIdAndDoctorAuthIdAndActiveTrue(patientId, doctorAuthId)
                .stream()
                .filter(access -> access.getExpiresAt() == null || access.getExpiresAt().isAfter(now))
                .flatMap(access -> access.getAllowedDocumentIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean canDoctorAccessDocument(String patientId, String doctorAuthId, String documentId) {
        return getAllowedDocumentIds(patientId, doctorAuthId).contains(documentId);
    }
}
