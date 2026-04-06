package com.aferent.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;      // client PUTs file directly to this URL
    private String documentId;     // client sends this back after upload completes
    private String minioKey;       // object_key returned by document-service
    private String permanentUrl;   // present for public objects only
    private String visibility;      // public or private
    private String category;       // category path segment used for storage
    private int expirySeconds;     // how long the URL is valid
}