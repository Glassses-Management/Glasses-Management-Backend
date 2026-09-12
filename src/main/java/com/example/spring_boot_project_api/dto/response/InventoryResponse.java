package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResponse {
    private Long id;

    private Long product_id;
    private Integer quantity;
    private Integer reorder_threshold;

    private LocalDateTime last_restocked_at;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    // Denormalised product info so the frontend can render inventory without
    // making a second request per row.
    private String sku;
    private String model;
    private String brand;
    private String category;
    private String color;
    private String size;
    private BigDecimal cost_price;
    private BigDecimal sale_price;
}
