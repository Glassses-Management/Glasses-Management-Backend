package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Request;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByCustomerId(Long customerId);

    List<Request> findByStatus(String status);

    Optional<Request> findById(Long id);
}
