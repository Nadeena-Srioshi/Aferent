package com.aferent.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalReportSummaryResponse {
    private String documentId;
    private String originalFileName;
    private String displayName;
    private LocalDateTime uploadedAt;
    private String contentType;
    private String documentType;
    private String documentSubType;
}
