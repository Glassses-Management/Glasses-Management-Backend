package com.example.spring_boot_project_api.exception;

/**
 * Raised when a Google identity proved who they are, but is not permitted to sign
 * in that way.
 *
 * <p>Used for the staff allowlist. The caller has already proven control of the
 * address, so returning the real reason is safe and is the only thing they can
 * act on.
 */
public class GoogleSignInNotPermittedException extends RuntimeException {

    public GoogleSignInNotPermittedException(String message) {
        super(message);
    }
}
