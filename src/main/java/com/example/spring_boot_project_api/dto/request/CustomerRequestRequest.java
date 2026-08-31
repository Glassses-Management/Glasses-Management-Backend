package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequestRequest {

    @NotBlank(message = "Request type is required")
    @Size(max = 20, message = "Request type must be under 20 characters")
    private String type;

    @Size(max = 1000, message = "Notes must be under 1000 characters")
    private String notes;

    private Long prescriptionId;
}