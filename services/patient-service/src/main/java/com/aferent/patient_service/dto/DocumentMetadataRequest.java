package com.aferent.patient_service.dto;

import lombok.Data;

@Data
public class DocumentMetadataRequest {
    private String documentId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String documentType;   // LAB_REPORT, PRESCRIPTION, SCAN etc
}