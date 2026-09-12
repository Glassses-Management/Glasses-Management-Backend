package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{
    List<OrderItem> findByOrderId(Long order_id);
    List<OrderItem> findByProductId(Long product_id);
}
