package com.aferent.doctor_service.service;

import com.aferent.doctor_service.dto.PatientVisitSummary;
import com.aferent.doctor_service.dto.PrescriptionPublicView;
import com.aferent.doctor_service.dto.PrescriptionRequest;
import com.aferent.doctor_service.exception.ForbiddenOperationException;
import com.aferent.doctor_service.exception.ResourceNotFoundException;
import com.aferent.doctor_service.kafka.DoctorEventProducer;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.Hospital;
import com.aferent.doctor_service.model.Prescription;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.HospitalRepository;
import com.aferent.doctor_service.repository.PrescriptionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorEventProducer eventProducer;
        private final DocumentServiceGateway documentServiceGateway;
        private final RestTemplate restTemplate;

    @Value("${app.prescription-base-url:http://localhost:8080}")
    private String prescriptionBaseUrl;

    // ─── issue prescription ──────────────────────────────────────────

    public Prescription issuePrescription(String doctorId, String authId,
                                           PrescriptionRequest request) {

        Doctor doctor = validateOwnershipAndActive(doctorId, authId);

        // validate consultation type
        boolean isInPerson = "IN_PERSON".equalsIgnoreCase(request.getConsultationType());
        boolean isVideo    = "VIDEO".equalsIgnoreCase(request.getConsultationType());

        if (!isInPerson && !isVideo) {
            throw new IllegalArgumentException(
                    "consultationType must be IN_PERSON or VIDEO");
        }

        // hospital validation
        String hospitalName = null;
        if (isInPerson) {
            if (request.getHospitalId() == null || request.getHospitalId().isBlank()) {
                throw new IllegalArgumentException(
                        "hospitalId is required for IN_PERSON consultations");
            }
            Hospital hospital = hospitalRepository.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid hospitalId: " + request.getHospitalId()));
            hospitalName = hospital.getName();
        }

        if (isVideo && request.getHospitalId() != null && !request.getHospitalId().isBlank()) {
            throw new IllegalArgumentException(
                    "hospitalId must not be provided for VIDEO consultations");
        }

        // generate prescriptionId
        String prescriptionId = generatePrescriptionId();

        // generate QR code and upload to MinIO
        String prescriptionUrl = prescriptionBaseUrl + "/prescriptions/" + prescriptionId + "/view";
        String qrKey = generateAndUploadQr(prescriptionId, prescriptionUrl);

        // build prescription document
        Prescription prescription = Prescription.builder()
                .prescriptionId(prescriptionId)
                .appointmentId(request.getAppointmentId())
                .consultationType(request.getConsultationType().toUpperCase())
                .doctorId(doctorId)
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .doctorSpecialization(doctor.getSpecialization())
                .hospitalId(isInPerson ? request.getHospitalId() : null)
                .hospitalName(isInPerson ? hospitalName : null)
                .patientId(request.getPatientId())
                .patientName(request.getPatientName())
                .patientAge(request.getPatientAge())
                .patientPhone(request.getPatientPhone())
                .patientEmail(request.getPatientEmail())
                .diagnosis(request.getDiagnosis())
                .symptoms(request.getSymptoms())
                .medications(request.getMedications())
                .notes(request.getNotes())
                .followUpDate(request.getFollowUpDate())
                .qrCodeKey(qrKey)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription issued prescriptionId={} doctorId={} patientId={}",
                prescriptionId, doctorId, request.getPatientId());

        // publish full prescription event for downstream medical history projection
        eventProducer.sendPrescriptionIssuedEvent(saved);

        // fire notifications
        fireNotifications(saved, prescriptionUrl);

        return saved;
    }

    // ─── doctor views ────────────────────────────────────────────────

    public List<Prescription> getDoctorPrescriptions(String doctorId, String authId) {
        validateOwnershipAndActive(doctorId, authId);
        return prescriptionRepository.findByDoctorIdOrderByIssuedAtDesc(doctorId);
    }

    public List<PatientVisitSummary> getDoctorPatients(String doctorId, String authId) {
        validateOwnershipAndActive(doctorId, authId);

        List<Prescription> all = prescriptionRepository.findByDoctorId(doctorId);

        // group by patientId and build summary
        Map<String, List<Prescription>> byPatient = all.stream()
                .collect(Collectors.groupingBy(Prescription::getPatientId));

        return byPatient.entrySet().stream()
                .map(entry -> {
                    List<Prescription> visits = entry.getValue();
                    Prescription latest = visits.stream()
                            .max(Comparator.comparing(Prescription::getIssuedAt))
                            .orElseThrow();
                    return PatientVisitSummary.builder()
                            .patientId(entry.getKey())
                            .patientName(latest.getPatientName())
                            .patientAge(latest.getPatientAge())
                            .visitCount(visits.size())
                            .lastVisitDate(latest.getIssuedAt())
                            .build();
                })
                .sorted(Comparator.comparing(PatientVisitSummary::getLastVisitDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Prescription> getPatientPrescriptions(String doctorId, String authId,
                                                        String patientId) {
        validateOwnershipAndActive(doctorId, authId);
        return prescriptionRepository
                .findByDoctorIdAndPatientIdOrderByIssuedAtDesc(doctorId, patientId);
    }

    // ─── public QR view ──────────────────────────────────────────────

    public PrescriptionPublicView getPublicView(String prescriptionId) {
        Prescription p = prescriptionRepository.findByPrescriptionId(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prescription not found: " + prescriptionId));

        return PrescriptionPublicView.builder()
                .prescriptionId(p.getPrescriptionId())
                .patientName(p.getPatientName())
                .patientAge(p.getPatientAge())
                .doctorName(p.getDoctorName())
                .doctorSpecialization(p.getDoctorSpecialization())
                .hospitalName(p.getHospitalName() != null
                        ? p.getHospitalName()
                        : "Video Consultation")
                .diagnosis(p.getDiagnosis())
                .symptoms(p.getSymptoms())
                .medications(p.getMedications())
                .notes(p.getNotes())
                .followUpDate(p.getFollowUpDate())
                .issuedAt(p.getIssuedAt())
                .build();
    }

        public String getPrescriptionQrSignedUrl(
                        String prescriptionId,
                        String requesterAuthId,
                        String requesterRole,
                        String requesterPatientId,
                        int expiresSeconds
        ) {
                Prescription prescription = prescriptionRepository.findByPrescriptionId(prescriptionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Prescription not found: " + prescriptionId));

                if (prescription.getQrCodeKey() == null || prescription.getQrCodeKey().isBlank()) {
                        throw new ResourceNotFoundException(
                                        "No QR document uploaded for prescriptionId=" + prescriptionId);
                }

                if (expiresSeconds <= 0 || expiresSeconds > 604800) {
                        throw new IllegalArgumentException("expires must be between 1 and 604800 seconds");
                }

                if ("DOCTOR".equalsIgnoreCase(requesterRole)) {
                        Doctor issuer = doctorRepository.findByDoctorId(prescription.getDoctorId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Doctor not found: " + prescription.getDoctorId()));
                        if (!issuer.getAuthId().equals(requesterAuthId)) {
                                throw new ForbiddenOperationException(
                                                "Only the issuing doctor can access this prescription QR URL");
                        }
                } else if ("PATIENT".equalsIgnoreCase(requesterRole)) {
                        boolean matchesPatient = prescription.getPatientId().equals(requesterAuthId)
                                        || (requesterPatientId != null && prescription.getPatientId().equals(requesterPatientId));
                        if (!matchesPatient) {
                                throw new ForbiddenOperationException(
                                                "Only the receiving patient can access this prescription QR URL");
                        }
                } else {
                        throw new ForbiddenOperationException(
                                        "Only the issuing doctor or receiving patient can access this prescription QR URL");
                }

                return documentServiceGateway.generateDownloadUrl(prescription.getQrCodeKey(), expiresSeconds);
        }

    // ─── QR generation ──────────────────────────────────────────────

    private String generateAndUploadQr(String prescriptionId, String url) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            byte[] qrBytes = out.toByteArray();
            log.info("Generated QR PNG for prescriptionId={} with {} bytes", prescriptionId, qrBytes.length);

            String category = "prescription-qr/" + prescriptionId;
            String fileName = prescriptionId + ".png";

            // Get presigned upload URL from document-service
            DocumentServiceGateway.PresignUploadResult presignResult = documentServiceGateway.generateUploadUrl(
                    "private",
                    category,
                    fileName,
                    false
            );

            String uploadTarget = (presignResult.internalUploadUrl() != null && !presignResult.internalUploadUrl().isBlank())
                    ? presignResult.internalUploadUrl()
                    : presignResult.uploadUrl();

                        // Upload QR bytes directly via presigned URL.
                        // Use URI-based request to preserve query params exactly as signed.
                        URI uploadUri = URI.create(uploadTarget);
                        RequestEntity<byte[]> uploadRequest = RequestEntity
                                        .put(uploadUri)
                                        .body(qrBytes);

                        try {
                                restTemplate.exchange(uploadRequest, Void.class);
                        } catch (Exception uploadEx) {
                                // Diagnostic aid: prove QR bytes were generated by writing a local temp file.
                                try {
                                        Path debugPath = Path.of(System.getProperty("java.io.tmpdir"), prescriptionId + ".png");
                                        Files.write(debugPath, qrBytes);
                                        log.error("QR upload failed for prescriptionId={}. Saved local QR at {}", prescriptionId, debugPath, uploadEx);
                                } catch (Exception ioEx) {
                                        log.error("QR upload failed for prescriptionId={} and failed to save local debug QR", prescriptionId, ioEx);
                                }
                                throw uploadEx;
                        }

            String objectKey = presignResult.objectKey();
            if (objectKey == null || objectKey.isBlank()) {
                throw new RuntimeException("Failed to get object key from document-service presign response");
            }

            log.info("QR uploaded to MinIO via document-service, prescriptionId={}, key={}", prescriptionId, objectKey);
            return objectKey;

        } catch (Exception e) {
            log.error("Failed to generate/upload QR for prescriptionId={}", prescriptionId, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    // ─── notifications ───────────────────────────────────────────────

    private void fireNotifications(Prescription p, String prescriptionUrl) {

        // Get signed download URL for private QR file
        String qrDownloadUrl = documentServiceGateway.generateDownloadUrl(p.getQrCodeKey(), 3600);
        String doctorDisplay = "Dr. " + p.getDoctorName();
        String hospitalDisplay = p.getHospitalName() != null
                ? p.getHospitalName() : "Video Consultation";

        // SMS notification
        Map<String, Object> sms = new HashMap<>();
        sms.put("sourceService", "doctor-service");
        sms.put("eventType", "prescription.issued");
        sms.put("channel", "SMS");
        sms.put("toPhone", p.getPatientPhone());
        sms.put("message", "Your prescription from " + doctorDisplay
                + " (" + hospitalDisplay + ") is ready. "
                + "View here: " + prescriptionUrl);
        eventProducer.sendNotification(sms);

        // Email notification
        Map<String, Object> email = new HashMap<>();
        email.put("sourceService", "doctor-service");
        email.put("eventType", "prescription.issued");
        email.put("channel", "EMAIL");
        email.put("toEmail", p.getPatientEmail());
        email.put("subject", "Your Prescription - " + doctorDisplay);
        email.put("message",
                "Dear " + p.getPatientName() + ",\n\n"
                + "Your prescription has been issued by " + doctorDisplay
                + " at " + hospitalDisplay + ".\n\n"
                + "Diagnosis: " + p.getDiagnosis() + "\n\n"
                + "Please find your prescription QR code at: " + prescriptionUrl + "\n\n"
                + "You can also download your QR image here: " + qrDownloadUrl + "\n\n"
                + (p.getFollowUpDate() != null
                        ? "Follow-up date: " + p.getFollowUpDate() + "\n\n"
                        : "")
                + "Thank you.");
        email.put("qrImageUrl", qrDownloadUrl);
        eventProducer.sendNotification(email);

        log.info("Notifications fired for prescriptionId={}", p.getPrescriptionId());
    }

    // ─── helpers ────────────────────────────────────────────────────

    private String generatePrescriptionId() {
        long count = prescriptionRepository.count();
        return String.format("RX_%04d", count + 1);
    }

    private Doctor validateOwnershipAndActive(String doctorId, String authId) {
        Doctor doctor = doctorRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + doctorId));

        if (!doctor.getAuthId().equals(authId)) {
            throw new ForbiddenOperationException(
                    "You can only manage your own prescriptions");
        }

        if (doctor.getStatus() != Doctor.RegistrationStatus.ACTIVE) {
            throw new ForbiddenOperationException(
                    "Account not active. Current status: " + doctor.getStatus());
        }

        return doctor;
    }
}