package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InventoryResponse {
    private Long id;

    private Long product_id;
    private Integer quantity;
    private Integer reorder_threshold;
    private Long branch_id;

    private LocalDateTime last_restocked_at;
}
