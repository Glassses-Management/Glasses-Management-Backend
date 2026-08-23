package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be under 20 character")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number must be under 15 character")
    private String phone;

    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be under 100 character")
    private String email;

    private String address;

    private LocalDate date_of_birth;

}
