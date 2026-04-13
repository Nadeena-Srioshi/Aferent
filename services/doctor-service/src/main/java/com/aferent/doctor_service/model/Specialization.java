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
    private boolean active;
}