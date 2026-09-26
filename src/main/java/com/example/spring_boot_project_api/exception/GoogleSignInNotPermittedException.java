package com.example.spring_boot_project_api.exception;

/**
 * A Google-verified identity that is not permitted to use this sign-in method.
 *
 * Separate from BadCredentialsException on purpose. Password login deliberately
 * answers with one generic message so it cannot be used to discover which
 * addresses have accounts, but that same message would hide the reason here:
 * the caller has already proven they control the email through Google, so
 * telling them the account is not on the staff allowlist leaks nothing useful
 * and is the only actionable thing they can be told.
 */
public class GoogleSignInNotPermittedException extends RuntimeException {

    public GoogleSignInNotPermittedException(String message) {
        super(message);
    }
}
