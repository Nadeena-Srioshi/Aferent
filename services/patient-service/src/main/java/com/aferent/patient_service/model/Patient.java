package com.aferent.patient_service.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patients")
public class Patient {

    @Id
    private String id;          // MongoDB object ID

    @Indexed(unique = true)
    private String authId;      // auth-service user ID (links to auth_db.users._id)

    @Indexed(unique = true)
    private String patientId;   // human-readable patient ID (e.g., PAT_001, PAT_002)

    @Indexed(unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private Address address;

    // embedded document — no separate collection needed for simple address
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String country;
    }

    @CreatedDate     // Spring automatically sets this on insert
    private LocalDateTime createdAt;

    @LastModifiedDate  // Spring automatically updates this on every save
    private LocalDateTime updatedAt;
}