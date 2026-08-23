package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    
    @NotBlank(message = "User name is required")
    @Size(max = 20, message = "User name must be under 20 character")
    private String userName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must be under 100 character")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must contains at least 8 character")
    @Pattern(
        regexp = "(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one number"
    )
    private String password_hash;

    @NotNull(message = "Branch ID is required")
    private Long branch_id;

    @NotBlank(message = "Role is required")
    private String role;

}
