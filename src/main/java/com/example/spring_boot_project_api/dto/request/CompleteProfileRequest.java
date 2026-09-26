package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for POST /api/customers/complete-profile.
 *
 * Separate from {@link CustomerRequest} on purpose: that type requires an email,
 * but this flow must not accept one. The email is already fixed by the verified
 * Google identity on the account, and letting the body override it would allow
 * a signed-in user to attach their profile to somebody else's address.
 */
@Data
public class CompleteProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be under 20 character")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number must be under 15 character")
    private String phone;

    private String address;

    private LocalDate date_of_birth;
}
