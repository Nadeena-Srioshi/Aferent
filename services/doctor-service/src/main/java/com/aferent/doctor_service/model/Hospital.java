package com.aferent.doctor_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "hospitals")
public class Hospital {

    @Id
    private String id;

    private String name;    // "Nawaloka Hospital"
    private String city;    // "Colombo"
    private boolean active;
}