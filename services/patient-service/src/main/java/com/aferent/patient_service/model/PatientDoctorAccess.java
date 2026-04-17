package com.aferent.patient_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_doctor_access")
@CompoundIndexes({
        @CompoundIndex(name = "idx_patient_doctor_active", def = "{'patientId': 1, 'doctorAuthId': 1, 'active': 1}"),
        @CompoundIndex(name = "idx_appointment", def = "{'appointmentId': 1}")
})
public class PatientDoctorAccess {

    @Id
    private String id;

    private String patientId;
    private String doctorId;
    private String doctorAuthId;
    private String appointmentId;

    @Builder.Default
    private List<String> allowedDocumentIds = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
