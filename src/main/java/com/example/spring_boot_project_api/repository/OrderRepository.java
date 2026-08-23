package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long>{
    List<Order> findByCustomerId(Long customer_id);
    List<Order> findByBranchId(Long branch_id);
    List<Order> findByStatus(String status);
}
