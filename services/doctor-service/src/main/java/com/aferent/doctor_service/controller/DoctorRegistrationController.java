package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.RegisterProfileRequest;
import com.aferent.doctor_service.dto.LicenseUploadUrlRequest;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.service.DoctorRegistrationService;
import com.aferent.doctor_service.service.DocumentServiceGateway;
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
    private final DocumentServiceGateway documentServiceGateway;

    // Step 2+3 of multi-step form — called right after POST /auth/register
    // No JWT required — whitelisted at gateway like /auth/register
    @PostMapping("/register/profile")
    public ResponseEntity<Doctor> completeProfile(
            @Valid @RequestBody RegisterProfileRequest request
    ) {
        return ResponseEntity.ok(registrationService.completeProfile(request));
    }

        // Get presigned URL to upload license document to document-service
        // Public endpoint: authId is passed in request body
    @PostMapping("/license-upload-url")
    public ResponseEntity<Map<String, String>> getLicenseUploadUrl(
                        @Valid @RequestBody LicenseUploadUrlRequest request
    ) {
                Doctor doctor = registrationService.getDoctorByAuthId(request.getAuthId());
        String category = "doctor-license/" + doctor.getDoctorId();

        DocumentServiceGateway.PresignUploadResult result = documentServiceGateway.generateUploadUrl(
                "private",
                category,
                request.getFileName(),
                false
        );

        return ResponseEntity.ok(Map.of(
                "uploadUrl", result.uploadUrl(),
                "objectKey", result.objectKey() != null ? result.objectKey() : ""
        ));
    }
}