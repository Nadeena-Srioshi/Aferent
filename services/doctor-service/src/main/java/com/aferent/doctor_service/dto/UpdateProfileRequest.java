package com.aferent.doctor_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private List<String> hospitalIds;  // list of hospital IDs from master collection
    private List<String> languages;
    private Integer yearsOfExperience;
    private List<String> qualifications;
}