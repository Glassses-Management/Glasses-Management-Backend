package com.example.spring_boot_project_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Entity
@Table(name = "product")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String model;
    private String brand;
    private String color;
    private String material;
    private String size;

    private Float cost_price;
    private Float sale_price;

    private String supplier_name;
    private String supplier_contact;
    private String category;

}
