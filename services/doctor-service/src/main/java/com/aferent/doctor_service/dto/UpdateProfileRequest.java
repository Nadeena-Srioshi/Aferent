package com.aferent.doctor_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String specialization;
    private String bio;
    private Integer yearsOfExperience;
    private List<String> hospitals;
    private List<String> qualifications;
}