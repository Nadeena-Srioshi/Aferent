package com.aferent.patient_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "medical_history")
public class MedicalHistory {

    @Id
    private String id;  // MongoDB object ID

    @Indexed(unique = true)
    private String patientId;  // Links to patient.patientId

    @Builder.Default
    private List<MedicalRecord> records = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Add a medical record to the history.
     * Maintains insertion order.
     */
    public void addRecord(MedicalRecord record) {
        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        record.setRecordedAt(LocalDateTime.now());
        this.records.add(record);
    }
}
