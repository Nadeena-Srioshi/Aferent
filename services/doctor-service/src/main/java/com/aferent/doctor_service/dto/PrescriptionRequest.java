package com.aferent.doctor_service.dto;

import com.aferent.doctor_service.model.Prescription.Medication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PrescriptionRequest {

    @NotBlank(message = "appointmentId is required")
    private String appointmentId;

    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotBlank(message = "patientName is required")
    private String patientName;

    @NotNull(message = "patientAge is required")
    private Integer patientAge;

    @NotBlank(message = "patientPhone is required")
    private String patientPhone;

    @NotBlank(message = "patientEmail is required")
    private String patientEmail;

    @NotBlank(message = "consultationType is required")
    private String consultationType; // IN_PERSON or VIDEO

    private String hospitalId; // required only for IN_PERSON

    @NotBlank(message = "diagnosis is required")
    private String diagnosis;

    private String symptoms;

    @NotEmpty(message = "At least one medication is required")
    private List<Medication> medications;

    private String notes;
    private LocalDate followUpDate;
}