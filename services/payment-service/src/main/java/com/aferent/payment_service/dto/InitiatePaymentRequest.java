package com.aferent.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;

    @NotNull(message = "Amount is required")
    private Double amount;

    @NotBlank(message = "Appointment type is required")
    private String appointmentType;   // PHYSICAL or VIDEO

    private String doctorId;
    private String patientEmail;
}
