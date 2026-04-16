package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doctors")
public class Doctor {

    @Id
    private String id;

    @Indexed(unique = true)
    private String authId;

    @Indexed(unique = true)
    private String doctorId;

    @Indexed(unique = true)
    private String email;

    // registration fields
    private String firstName;
    private String lastName;
    private String phone;
    private String specialization;
    private String licenseNumber;
    private String licenseDocKey;
    private Integer yearsOfExperience;
    private List<String> qualifications;

    // profile fields — filled after login
    private String bio;
    private String profilePicKey;
    private String profilePicUrl;  // permanent URL for public profile picture
    private List<String> hospitals;
    private Double consultationFee;
    private List<String> languages;

    private RegistrationStatus status;

    public enum RegistrationStatus {
        PENDING_PROFILE,
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED
    }

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}