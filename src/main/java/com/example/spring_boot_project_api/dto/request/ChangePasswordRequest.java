package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must contains at least 8 character")
    @Pattern(
        regexp = "(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one number"
    )
    private String newPassword;

}