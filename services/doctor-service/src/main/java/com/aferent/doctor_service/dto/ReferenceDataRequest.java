package com.aferent.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReferenceDataRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String city;      // only used for hospitals

    private boolean active = true;
}