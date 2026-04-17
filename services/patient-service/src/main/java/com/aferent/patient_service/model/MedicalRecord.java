package com.aferent.patient_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    private String prescriptionId;
    private String appointmentId;
    private String consultationType;

    // Doctor info
    private String doctorId;
    private String doctorName;
    private String doctorSpecialization;

    // Hospital info
    private String hospitalId;
    private String hospitalName;

    // Patient info (denormalized for context)
    private String patientId;
    private String patientName;
    private Integer patientAge;
    private String patientPhone;
    private String patientEmail;

    // Medical details
    private String diagnosis;
    private List<String> symptoms;
    private List<Map<String, Object>> medications;
    private String notes;
    private String followUpDate;

    // QR Code
    private String qrCodeKey;

    // Timestamps
    private LocalDateTime issuedAt;
    private LocalDateTime recordedAt;  // when this was stored in medical history
}
