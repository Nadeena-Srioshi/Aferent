package com.aferent.patient_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareDocumentsRequest {

    @NotBlank(message = "appointmentId is required")
    private String appointmentId;

    @NotBlank(message = "doctorId is required")
    private String doctorId;

    @NotBlank(message = "doctorAuthId is required")
    private String doctorAuthId;

    @NotEmpty(message = "documentIds must not be empty")
    private List<String> documentIds;

    private LocalDateTime expiresAt;
}
