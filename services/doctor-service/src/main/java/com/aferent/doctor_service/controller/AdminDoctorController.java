package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.VerifyDoctorRequest;
import com.aferent.doctor_service.model.Doctor;
import com.aferent.doctor_service.service.AdminDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    @GetMapping("/pending")
    public ResponseEntity<List<Doctor>> getPendingDoctors(
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(adminDoctorService.getPendingDoctors());
    }

    @PatchMapping("/{doctorId}/verify")
    public ResponseEntity<Doctor> verifyDoctor(
            @PathVariable String doctorId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody VerifyDoctorRequest request
    ) {
        return ResponseEntity.ok(adminDoctorService.verifyDoctor(doctorId, request, role));
    }
}