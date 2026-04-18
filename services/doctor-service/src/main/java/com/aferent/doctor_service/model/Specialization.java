package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "specializations")
public class Specialization {

    @Id
    private String id;

    private String name;   // "Cardiology", "Neurology", "Other"
    private Double maxVideoConsultationFee;      // max fee for 15-min video consultation
    private Double maxPhysicalConsultationFee;   // max fee for 15-min physical consultation
    private boolean active;
}