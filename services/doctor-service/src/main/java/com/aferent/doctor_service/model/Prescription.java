package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "prescriptions")
public class Prescription {

    @Id
    private String id;

    @Indexed(unique = true)
    private String prescriptionId;   // RX_001, RX_002...

    // appointment reference
    private String appointmentId;
    private String consultationType; // IN_PERSON or VIDEO

    // doctor details (snapshot at time of issue)
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;

    // hospital details (snapshot — null for VIDEO)
    private String hospitalId;
    private String hospitalName;

    // patient details (from frontend/appointment)
    @Indexed
    private String patientId;
    private String patientName;
    private Integer patientAge;
    private String patientPhone;
    private String patientEmail;

    // medical details
    private String diagnosis;
    private String symptoms;
    private List<Medication> medications;
    private String notes;
    private LocalDate followUpDate;

    // QR
    private String qrCodeKey;   // MinIO object key

    @CreatedDate
    private LocalDateTime issuedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Medication {
        private String name;
        private String dosage;       // "500mg"
        private String frequency;    // "twice daily"
        private String duration;     // "7 days"
        private String instructions; // "after meals"
    }
}