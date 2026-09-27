package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Carries the ID token that Google Identity Services returns in the browser.
 *
 * <p>The value is verified server side against Google's public keys and is never
 * trusted as-is. It is a JWT, not an email address: the address is read from the
 * verified token, never taken from the request.
 */
@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;
}
