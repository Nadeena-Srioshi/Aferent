package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.UpdateProfileRequest;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.model.Hospital;
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
    public ResponseEntity<List<Doctor>> getAllActiveDoctors(
            @RequestParam(required = false) String specializationId,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String hospital,
            @RequestParam(required = false) String date
    ) {
        return ResponseEntity.ok(profileService.searchActiveDoctors(specializationId, specialty, name, hospital, date));
    }

    // get any doctor profile — patients and admins can also call this
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getProfile(
            @PathVariable String doctorId
    ) {
        return ResponseEntity.ok(profileService.getProfile(doctorId));
    }

    // get hospitals assigned to authenticated doctor
    @GetMapping("/me/hospitals")
    public ResponseEntity<List<Hospital>> getMyHospitals(
            @RequestHeader("X-User-ID") String authId
    ) {
        return ResponseEntity.ok(profileService.getMyHospitals(authId));
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

    // Get presigned URL to upload profile picture
    // Backend stores the permanent URL immediately upon presign request
    // Frontend uploads directly to MinIO using the uploadUrl
    // Identity comes from X-User-ID because the caller is already authenticated
    @PostMapping("/profile/pic-upload-url")
    public ResponseEntity<Map<String, String>> getProfilePicUploadUrl(
            @RequestHeader("X-User-ID") String authId,
            @RequestParam String fileName
    ) {
        return ResponseEntity.ok(profileService.getProfilePicUploadUrl(authId, fileName));
    }

    // Get profile picture permanent URL (for public profile picture)
    // Returns the public URL stored when the picture was uploaded
    @GetMapping("/{doctorId}/profile/pic-url")
    public ResponseEntity<Map<String, String>> getProfilePicUrl(
            @PathVariable String doctorId
    ) {
        String url = profileService.getProfilePicUrl(doctorId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // Get signed URL for private license document
    // Accessible to the doctor owner and admins only
    @GetMapping("/{doctorId}/license/signed-url")
    public ResponseEntity<Map<String, String>> getLicenseSignedUrl(
            @PathVariable String doctorId,
            @RequestHeader("X-User-ID") String authId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "3600") int expires
    ) {
        String url = profileService.getLicenseSignedUrl(doctorId, authId, role, expires);
        return ResponseEntity.ok(Map.of("url", url));
    }
}