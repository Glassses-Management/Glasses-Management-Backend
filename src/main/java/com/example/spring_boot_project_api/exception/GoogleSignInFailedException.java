package com.example.spring_boot_project_api.exception;

/**
 * Raised when a Google ID token cannot be trusted.
 *
 * <p>Deliberately distinct from BadCredentialsException. Password login masks its
 * failures to avoid revealing whether an account exists, but that masking is
 * actively harmful for a token failure: a client-id mismatch, an expired token and
 * a request that sent an email address instead of a token all look identical
 * behind BadCredentialsException, which makes the integration impossible to
 * debug. Nothing account-specific is disclosed, only the reason the token was
 * refused.
 */
public class GoogleSignInFailedException extends RuntimeException {

    public GoogleSignInFailedException(String message) {
        super(message);
    }

    public GoogleSignInFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
