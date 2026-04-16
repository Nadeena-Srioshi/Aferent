package com.aferent.doctor_service.repository;

import com.aferent.doctor_service.model.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HospitalRepository extends MongoRepository<Hospital, String> {
    List<Hospital> findByActiveTrue();
}