package com.aferent.patient_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_id_sequence")
public class PatientIdSequence {

    @Id
    private String id;          // fixed: "patientIdCounter"

    private Long sequence;      // current sequence number
}
