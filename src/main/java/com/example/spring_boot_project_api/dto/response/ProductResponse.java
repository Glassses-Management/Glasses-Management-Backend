package com.example.spring_boot_project_api.dto.response;

import lombok.Data;

@Data
public class ProductResponse {
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
