package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be under 20 character")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number must be under 15 character")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be under 100 character")
    @Email(message = "Invalid email")
    private String email;

    private String address;

    private LocalDate date_of_birth;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
