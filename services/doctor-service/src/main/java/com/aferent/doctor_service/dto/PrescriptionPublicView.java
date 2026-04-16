package com.aferent.doctor_service.dto;

import com.aferent.doctor_service.model.Prescription.Medication;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrescriptionPublicView {
    private String prescriptionId;

    // patient — no phone or email exposed
    private String patientName;
    private Integer patientAge;

    // doctor
    private String doctorName;
    private String doctorSpecialization;

    // hospital or video
    private String hospitalName; // "Video Consultation" if VIDEO

    // medical
    private String diagnosis;
    private String symptoms;
    private List<Medication> medications;
    private String notes;
    private LocalDate followUpDate;

    private LocalDateTime issuedAt;
}