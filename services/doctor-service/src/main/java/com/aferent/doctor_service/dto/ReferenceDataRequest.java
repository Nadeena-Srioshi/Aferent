package com.aferent.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ReferenceDataRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String city;      // only used for hospitals

    @PositiveOrZero(message = "Max video consultation fee must be 0 or greater")
    private Double maxVideoConsultationFee;

    @PositiveOrZero(message = "Max physical consultation fee must be 0 or greater")
    private Double maxPhysicalConsultationFee;

    private boolean active = true;
}