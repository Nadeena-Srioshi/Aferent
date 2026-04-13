package com.aferent.patient_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_documents")
public class PatientDocument {

    @Id
    private String id;

    @Indexed
    private String patientId;    // which patient owns this file

    private String fileName;
    private String originalFileName;
    private String contentType;   // e.g. "application/pdf", "image/jpeg"
    private Long fileSize;        // in bytes
    private String minioKey;      // the path inside MinIO bucket
    private String documentType;  // e.g. "LAB_REPORT", "PRESCRIPTION", "SCAN"
    @Builder.Default
    private boolean deleted = false;  // soft delete — never actually remove

    @CreatedDate
    private LocalDateTime uploadedAt;
}