package com.aferent.auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    @Default
    private boolean active = true;

    @Default
    private long refreshTokenVersion = 1L;

    private Instant activatedAt;
    private String activatedBy;
    private Instant deactivatedAt;
    private String deactivatedBy;
    private String deactivationReason;

    public enum Role {
        PATIENT, DOCTOR, ADMIN
    }
}