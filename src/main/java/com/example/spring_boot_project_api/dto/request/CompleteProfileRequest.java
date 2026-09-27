package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Details for an account that has no customer profile yet.
 *
 * <p>Deliberately has no email field: the address comes from the verified token
 * so it can never be set from the request body.
 */
@Data
public class CompleteProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be under 20 character")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number must be under 15 character")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,15}$", message = "Invalid phone number")
    private String phone;

    @Size(max = 100, message = "Address must be under 100 character")
    private String address;

    private LocalDate date_of_birth;
}
