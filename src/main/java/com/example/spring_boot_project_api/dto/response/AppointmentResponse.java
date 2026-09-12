package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AppointmentResponse {
    private Long id;
    private Long customer_id;
    private Long optometrist_id;

    private LocalDateTime scheduled_at;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}
