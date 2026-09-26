package com.example.spring_boot_project_api.exception;

/**
 * Raised when a Google ID token cannot be trusted.
 *
 * Distinct from BadCredentialsException on purpose. Password login masks its
 * failures as "Invalid email or password" to avoid revealing whether an account
 * exists, but that masking is actively harmful for a Google token: the caller
 * cannot fix a client-id mismatch or a malformed token without knowing which
 * one occurred, and the browser only ever sees the generic message. Nothing
 * user-specific is disclosed here, only the reason the token was refused.
 */
public class GoogleSignInFailedException extends RuntimeException {

    public GoogleSignInFailedException(String message) {
        super(message);
    }

    public GoogleSignInFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
