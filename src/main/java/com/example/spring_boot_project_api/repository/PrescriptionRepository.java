package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.Prescription;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long>{
    List<Prescription> findByCustomerId(Long customer_id);
}
