package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AppointmentRequest {

    @NotNull(message = "Customer id is required")
    private Long customer_id;

    private Long optometrist_id;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduled_at;

    @NotBlank(message = "Status is require")
    @Size(max = 20, message = "Status must be under 20 characters")
    private String status;

    @Size(max = 255, message = "Notes must be under 255 characters")
    private String notes;
}
