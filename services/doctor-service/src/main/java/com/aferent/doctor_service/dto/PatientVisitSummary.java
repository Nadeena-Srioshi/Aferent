package com.aferent.doctor_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PatientVisitSummary {
    private String patientId;
    private String patientName;
    private Integer patientAge;
    private long visitCount;
    private LocalDateTime lastVisitDate;
}