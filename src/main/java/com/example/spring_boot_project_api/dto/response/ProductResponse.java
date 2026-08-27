package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long id;

    private String sku;
    private String model;
    private String brand;
    private String color;
    private String material;
    private String size;

    private BigDecimal cost_price;
    private BigDecimal sale_price;

    private String supplier_name;
    private String supplier_contact;
    private String category;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
