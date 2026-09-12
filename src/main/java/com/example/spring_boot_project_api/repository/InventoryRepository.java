package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findAllByProductId(Long productId);

    List<Inventory> findByQuantityLessThanEqual(Integer quantity);

    Optional<Inventory> findByProductId(Long productId);
}
