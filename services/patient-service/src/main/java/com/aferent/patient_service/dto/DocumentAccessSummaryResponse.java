package com.aferent.patient_service.dto;

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
public class DocumentAccessSummaryResponse {
    private String accessId;
    private String doctorId;
    private String doctorAuthId;
    private String appointmentId;
    private List<String> allowedDocumentIds;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
