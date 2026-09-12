package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or phone is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
