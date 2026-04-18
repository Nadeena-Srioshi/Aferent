package com.aferent.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class RegisterProfileRequest {

    @NotBlank(message = "authId is required")
    private String authId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotNull(message = "Years of experience is required")
    private Integer yearsOfExperience;

    @NotNull(message = "consultationFee is required")
    @Valid
    private ConsultationFeeRequest consultationFee;

    private List<String> qualifications;

    @Data
    public static class ConsultationFeeRequest {
        @NotNull(message = "Video consultation fee is required")
        @Positive(message = "Video consultation fee must be greater than 0")
        private Double video;

        @NotNull(message = "Physical consultation fee is required")
        @Positive(message = "Physical consultation fee must be greater than 0")
        private Double physical;
    }
}