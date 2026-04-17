package com.aferent.doctor_service.kafka;

import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.Prescription;
import com.aferent.doctor_service.repository.DoctorRepository;
import com.aferent.doctor_service.repository.PrescriptionRepository;
import com.aferent.doctor_service.service.DoctorRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventConsumer {

    private final DoctorRegistrationService doctorRegistrationService;
    private final DoctorRepository doctorRepository;
    private final PrescriptionRepository prescriptionRepository;

    @KafkaListener(topics = "user.registered", groupId = "doctor-service-group")
    public void onUserRegistered(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();
        String role = (String) event.get("role");

        // only handle DOCTOR role
        if (!"DOCTOR".equals(role)) {
            return;
        }

        String authId = (String) event.get("authId");
        String email  = (String) event.get("email");

        log.info("Received user.registered for doctor authId={}", authId);
        doctorRegistrationService.createPendingProfile(authId, email);
    }

    /**
     * Listens to MinIO object creation events and updates document status in database.
     * Triggered when files are uploaded to MinIO.
     *
     * Handles:
     * - doctor-license uploads: Updates Doctor.licenseDocKey
     * - profile-picture uploads: Kafka confirms upload (URL already stored in pic-upload-url)
     * - prescription-qr uploads: Updates Prescription.qrCodeKey
     */
    @KafkaListener(topics = "${kafka-topics.minio-events:minio.events}", groupId = "doctor-service-group")
    public void onMinioObjectCreated(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) event.get("Records");
            if (records == null || records.isEmpty()) {
                log.warn("MinIO event has no Records");
                return;
            }

            for (Map<String, Object> rec : records) {
                String eventName = (String) rec.get("eventName");
                if (eventName == null || !eventName.startsWith("s3:ObjectCreated")) {
                    continue;
                }

                // Extract objectKey from nested s3 structure
                @SuppressWarnings("unchecked")
                Map<String, Object> s3 = (Map<String, Object>) rec.get("s3");
                if (s3 == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> object = (Map<String, Object>) s3.get("object");
                if (object == null) continue;

                String objectKey = (String) object.get("key");
                if (objectKey == null || objectKey.isBlank()) {
                    continue;
                }

                processDocumentUpload(normalizeObjectKey(objectKey));
            }
        } catch (Exception e) {
            log.error("Error processing MinIO event", e);
        }
    }

    private String normalizeObjectKey(String rawObjectKey) {
        String decodedKey;
        try {
            decodedKey = URLDecoder.decode(rawObjectKey, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.debug("Failed to decode MinIO object key '{}': {}", rawObjectKey, ex.getMessage());
            decodedKey = rawObjectKey;
        }

        // Some MinIO events include the bucket name as the first segment:
        // app-storage/doctor-service/private/doctor-license/DOC_014/license.pdf
        // We store and match against the path inside the bucket:
        // doctor-service/private/doctor-license/DOC_014/license.pdf
        String bucketPrefix = "app-storage/";
        if (decodedKey.startsWith(bucketPrefix)) {
            return decodedKey.substring(bucketPrefix.length());
        }

        return decodedKey;
    }

    private void processDocumentUpload(String objectKey) {
        log.info("Processing MinIO upload: {}", objectKey);

        // Object key format: {service-id}/{visibility}/{category}/{filename}
        // For doctor-service: doctor-service/private/doctor-license/{doctorId}/...
        //                   doctor-service/public/profile-picture/{doctorId}/...
        //                   doctor-service/private/prescription-qr/{prescriptionId}/...

        String[] parts = objectKey.split("/");
        if (parts.length < 4) {
            log.warn("Unexpected object key format: {}", objectKey);
            return;
        }

        String category = parts[2];

        try {
            if ("doctor-license".equals(category)) {
                handleLicenseUpload(objectKey, parts);
            } else if ("profile-picture".equals(category)) {
                handleProfilePictureUpload(objectKey, parts);
            } else if ("prescription-qr".equals(category)) {
                handlePrescriptionQrUpload(objectKey, parts);
            }
        } catch (Exception e) {
            log.error("Error handling document upload for objectKey={}", objectKey, e);
        }
    }

    private void handleLicenseUpload(String objectKey, String[] parts) {
        if (parts.length < 4) {
            log.warn("License upload: unexpected key format {}", objectKey);
            return;
        }

        String doctorId = parts[3];

        // Find doctor by doctorId and update license key
        doctorRepository.findByDoctorId(doctorId).ifPresent(doctor -> {
            doctor.setLicenseDocKey(objectKey);
            doctorRepository.save(doctor);
            log.info("License document confirmed for doctorId={}", doctorId);
        });
    }

    private void handleProfilePictureUpload(String objectKey, String[] parts) {
        if (parts.length < 4) {
            log.warn("Profile picture upload: unexpected key format {}", objectKey);
            return;
        }

        String doctorId = parts[3];

        // Profile picture URL is already stored in pic-upload-url endpoint
        // This event just confirms the upload was successful
        log.info("Profile picture upload confirmed for doctorId={}", doctorId);
    }

    private void handlePrescriptionQrUpload(String objectKey, String[] parts) {
        if (parts.length < 4) {
            log.warn("Prescription QR upload: unexpected key format {}", objectKey);
            return;
        }

        String prescriptionFilename = parts[3];
        String prescriptionId = prescriptionFilename.replace(".png", "");

        // Update prescription with qrCodeKey
        prescriptionRepository.findByPrescriptionId(prescriptionId).ifPresent(prescription -> {
            prescription.setQrCodeKey(objectKey);
            prescriptionRepository.save(prescription);
            log.info("Prescription QR confirmed for prescriptionId={}", prescriptionId);
        });
    }
}
