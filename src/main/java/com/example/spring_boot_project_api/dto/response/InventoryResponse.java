package com.example.spring_boot_project_api.dto.response;

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
}
