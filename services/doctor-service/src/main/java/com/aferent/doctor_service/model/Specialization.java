package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}