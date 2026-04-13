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
    private String permanentUrl;   // present for public objects only
}