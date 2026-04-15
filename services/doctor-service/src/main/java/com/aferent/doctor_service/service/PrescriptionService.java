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
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final MinioService minioService;
    private final MinioClient minioClient;
    private final DoctorEventProducer eventProducer;

    @Value("${minio.bucket}")
    private String bucket;

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

    // ─── QR generation ──────────────────────────────────────────────

    private String generateAndUploadQr(String prescriptionId, String url) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            byte[] qrBytes = out.toByteArray();

            String objectKey = "prescriptions/qr/" + prescriptionId + ".png";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(qrBytes),
                                    qrBytes.length, -1)
                            .contentType("image/png")
                            .build()
            );

            log.info("QR uploaded to MinIO key={}", objectKey);
            return objectKey;

        } catch (Exception e) {
            log.error("Failed to generate/upload QR for prescriptionId={}", prescriptionId, e);
            throw new RuntimeException("Failed to generate QR code");
        }
    }

    // ─── notifications ───────────────────────────────────────────────

    private void fireNotifications(Prescription p, String prescriptionUrl) {

        String qrDownloadUrl = minioService.generateDownloadUrl(p.getQrCodeKey());
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