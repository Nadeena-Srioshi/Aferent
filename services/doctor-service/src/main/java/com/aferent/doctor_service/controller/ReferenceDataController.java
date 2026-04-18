package com.aferent.doctor_service.controller;

import com.aferent.doctor_service.dto.ReferenceDataRequest;
import com.aferent.doctor_service.dto.SpecializationResponse;
import com.aferent.doctor_service.model.Hospital;
import com.aferent.doctor_service.model.Specialization;
import com.aferent.doctor_service.service.ReferenceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    // ─── hospitals — public GET, admin POST/PUT/DELETE ────────────────

    @GetMapping("/hospitals")
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(referenceDataService.getAllHospitals());
    }

    @PostMapping("/admin/hospitals")
    public ResponseEntity<Hospital> addHospital(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ReferenceDataRequest request
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(referenceDataService.addHospital(request));
    }

    @PutMapping("/admin/hospitals/{id}")
    public ResponseEntity<Hospital> updateHospital(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ReferenceDataRequest request
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(referenceDataService.updateHospital(id, request));
    }

    @DeleteMapping("/admin/hospitals/{id}")
    public ResponseEntity<Void> deleteHospital(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        referenceDataService.deleteHospital(id);
        return ResponseEntity.noContent().build();
    }

    // ─── specializations — public GET, admin POST/PUT/DELETE ──────────

    @GetMapping("/specializations")
    public ResponseEntity<List<SpecializationResponse>> getAllSpecializations(
            @RequestParam(name = "include_fees", defaultValue = "false") boolean includeFees
    ) {
        return ResponseEntity.ok(referenceDataService.getAllSpecializations(includeFees));
    }

    @PostMapping("/admin/specializations")
    public ResponseEntity<Specialization> addSpecialization(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ReferenceDataRequest request
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(referenceDataService.addSpecialization(request));
    }

    @PutMapping("/admin/specializations/{id}")
    public ResponseEntity<Specialization> updateSpecialization(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ReferenceDataRequest request
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(referenceDataService.updateSpecialization(id, request));
    }

    @DeleteMapping("/admin/specializations/{id}")
    public ResponseEntity<Void> deleteSpecialization(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String role
    ) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        referenceDataService.deleteSpecialization(id);
        return ResponseEntity.noContent().build();
    }
}