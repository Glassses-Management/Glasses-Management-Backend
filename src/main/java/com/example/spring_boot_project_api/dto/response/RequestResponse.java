package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RequestResponse {
    private Long id;
    private Long customer_id;
    private String customer_name;
    private String type;
    private String status;
    private String notes;
    private Long prescription_id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
