package com.aferent.doctor_service.repository;

import com.aferent.doctor_service.model.Specialization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpecializationRepository extends MongoRepository<Specialization, String> {
    List<Specialization> findByActiveTrue();
}