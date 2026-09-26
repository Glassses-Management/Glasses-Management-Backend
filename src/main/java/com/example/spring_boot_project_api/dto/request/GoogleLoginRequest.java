package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Carries the ID token that Google Identity Services returns in the browser.
 * The value is verified server side and is never trusted as-is.
 */
@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;
}
