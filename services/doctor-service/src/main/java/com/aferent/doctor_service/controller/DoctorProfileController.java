package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.UpdateProfileRequest;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.service.DoctorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileService profileService;

    @GetMapping
    public ResponseEntity<List<Doctor>> getAllActiveDoctors() {
        return ResponseEntity.ok(profileService.getAllActiveDoctors());
    }

    // get any doctor profile — patients and admins can also call this
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getProfile(
            @PathVariable String doctorId
    ) {
        return ResponseEntity.ok(profileService.getProfile(doctorId));
    }

    // update own profile
    @PutMapping("/{doctorId}/profile")
    public ResponseEntity<Doctor> updateProfile(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(doctorId, authId, request));
    }

    // step 1 — get presigned URL to upload profile picture
    @PostMapping("/{doctorId}/profile/pic-upload-url")
    public ResponseEntity<Map<String, String>> getProfilePicUploadUrl(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        String url = profileService.getProfilePicUploadUrl(doctorId, authId, fileName, contentType);
        String objectKey = "profile-pics/" + doctorId + "/" + fileName;
        return ResponseEntity.ok(Map.of(
                "uploadUrl", url,
                "objectKey", objectKey
        ));
    }

    // step 2 — confirm profile picture upload
    @PostMapping("/{doctorId}/profile/pic-confirm")
    public ResponseEntity<Doctor> confirmProfilePicUpload(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @RequestParam String objectKey
    ) {
        return ResponseEntity.ok(
                profileService.confirmProfilePicUpload(doctorId, authId, objectKey));
    }

    // get profile picture download URL
    @GetMapping("/{doctorId}/profile/pic-url")
    public ResponseEntity<Map<String, String>> getProfilePicUrl(
            @PathVariable String doctorId
    ) {
        String url = profileService.getProfilePicDownloadUrl(doctorId);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }
}