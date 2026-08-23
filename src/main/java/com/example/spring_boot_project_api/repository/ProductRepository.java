package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    List<Product> findByCategory(String category);
    List<Product> findByBrand(String brand);
}
