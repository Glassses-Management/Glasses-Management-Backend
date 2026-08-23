package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
