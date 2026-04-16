package com.aferent.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyDoctorRequest {

    @NotBlank(message = "Action is required")
    private String action; // approve or reject

    private String reason; // optional — used when rejecting
}