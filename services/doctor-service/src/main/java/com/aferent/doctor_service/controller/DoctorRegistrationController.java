package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.RegisterProfileRequest;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.service.DoctorRegistrationService;
import com.aferent.doctor_service.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorRegistrationController {

    private final DoctorRegistrationService registrationService;
    private final MinioService minioService;

    // Step 2+3 of multi-step form — called right after POST /auth/register
    // No JWT required — whitelisted at gateway like /auth/register
    @PostMapping("/register/profile")
    public ResponseEntity<Doctor> completeProfile(
            @Valid @RequestBody RegisterProfileRequest request
    ) {
        return ResponseEntity.ok(registrationService.completeProfile(request));
    }

    // Get presigned URL to upload license document to MinIO
    // No JWT required — doctor hasn't been verified yet
    @PostMapping("/register/license-upload-url")
    public ResponseEntity<Map<String, String>> getLicenseUploadUrl(
            @RequestParam String authId,
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        String objectKey = "licenses/" + authId + "/" + fileName;
        String url = minioService.generateUploadUrl(objectKey, contentType);
        return ResponseEntity.ok(Map.of(
                "uploadUrl", url,
                "objectKey", objectKey
        ));
    }

    // Save license object key after frontend finishes uploading to MinIO
    @PostMapping("/register/license-confirm")
    public ResponseEntity<Doctor> confirmLicenseUpload(
            @RequestParam String authId,
            @RequestParam String objectKey
    ) {
        return ResponseEntity.ok(registrationService.saveLicenseKey(authId, objectKey));
    }
}