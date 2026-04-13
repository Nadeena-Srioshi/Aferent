package com.aferent.patient_service.dto;

import com.aferent.patient_service.model.Patient;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private Patient.Address address;
}