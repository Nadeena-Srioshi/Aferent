package com.aferent.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LicenseUploadUrlRequest {

    @NotBlank(message = "authId is required")
    private String authId;

    @NotBlank(message = "fileName is required")
    private String fileName;
}
