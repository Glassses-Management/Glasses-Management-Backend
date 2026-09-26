package com.example.spring_boot_project_api.security;

import com.example.spring_boot_project_api.exception.GoogleSignInFailedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Component;

/**
 * Verifies the ID token that Google Identity Services hands to the browser.
 *
 * The browser is never trusted: this class re-checks the signature against
 * Google's published keys, the issuer, the expiry, and the audience. The client
 * id is read from configuration and never taken from the request, so a token
 * minted for a different application is rejected.
 *
 * Deliberately not exposed as a {@code JwtDecoder} bean: SecurityConfig already
 * declares one for this app's own tokens, and a second bean of that type would
 * make the resource-server wiring ambiguous.
 */
@Component
public class GoogleTokenVerifier {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GoogleTokenVerifier.class);

    /** Google's current signing keys. Fetched and cached on first use. */
    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /** Google issues with or without the scheme depending on the flow. */
    private static final String ISSUER = "https://accounts.google.com";
    private static final String ISSUER_WITHOUT_SCHEME = "accounts.google.com";

    /** The claim naming the application a token was minted for. */
    private static final String AUDIENCE_CLAIM = "aud";

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();

        if (this.clientId.isEmpty()) {
            // Surfaced at startup on purpose. Left to the first sign-in attempt
            // this only appears as a failed login, which looks like a bad client
            // id rather than a missing environment variable.
            log.warn("Google sign-in is DISABLED: no client id configured. Set GOOGLE_CLIENT_ID "
                    + "in the environment (for Docker, add it to the app service in "
                    + "docker-compose.yml, which only forwards variables listed there).");
        }

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
                // Pinning the algorithm blocks "alg" confusion attacks, where a
                // token is re-signed as HMAC using the public key as the secret.
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                this::validateIssuerAndAudience));

        this.decoder = nimbus;
    }

    public boolean isConfigured() {
        return !clientId.isEmpty();
    }

    /**
     * @throws IllegalStateException when no client id is configured, so the caller
     *         gets a clear configuration error instead of an auth failure.
     * @throws BadCredentialsException equivalent via JwtException when the token is
     *         expired, forged, or issued for another application.
     */
    public Jwt verify(String credential) {
        if (!isConfigured()) {
            throw new GoogleSignInFailedException(
                    "Google sign-in is not configured on this server (GOOGLE_CLIENT_ID is missing)");
        }
        if (credential == null || credential.isBlank()) {
            throw new GoogleSignInFailedException(
                    "No Google credential was sent. Sign-in must pass the ID token from "
                            + "response.credential, not an email address.");
        }

        // A credential that is not a JWT at all is the single most common
        // integration mistake, so it is named explicitly instead of being
        // reported as a generic authentication failure.
        if (credential.trim().split("\\.").length != 3) {
            log.warn("Google sign-in rejected: credential is not a JWT (three dot-separated parts "
                    + "expected). An email address or access token cannot be used here.");
            throw new GoogleSignInFailedException(
                    "The credential sent is not a Google ID token. Send the value from "
                            + "response.credential.");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(credential.trim());
        } catch (JwtException ex) {
            // The reason is logged, never the token. A rejected audience is
            // separated out because it is a server misconfiguration rather than
            // a bad token, and the fix is completely different.
            log.warn("Google sign-in rejected: token could not be verified ({}: {})",
                    ex.getClass().getSimpleName(), ex.getMessage());
            boolean audienceProblem = ex.getMessage() != null
                    && ex.getMessage().toLowerCase().contains("audience");
            throw new GoogleSignInFailedException(audienceProblem
                    ? "This Google token was issued for a different application than this server "
                    + "expects. Check that GOOGLE_CLIENT_ID and VITE_GOOGLE_CLIENT_ID match."
                    : "The Google token could not be verified. It may have expired or been tampered with.",
                    ex);
        }

        // Google only returns a verified address when this is true. Without this
        // check an unverified address could be used to take over an account.
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            log.warn("Google sign-in rejected: account email is not verified");
            throw new GoogleSignInFailedException("This Google account's email is not verified.");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            log.warn("Google sign-in rejected: token carried no email claim");
            throw new GoogleSignInFailedException("The Google token did not contain an email address.");
        }

        return jwt;
    }

    public String emailFrom(Jwt jwt) {
        return jwt.getClaimAsString("email").trim().toLowerCase();
    }

    public String nameFrom(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? null : name.trim();
    }

    private OAuth2TokenValidatorResult validateIssuerAndAudience(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (!ISSUER.equals(issuer) && !ISSUER_WITHOUT_SCHEME.equals(issuer)) {
            log.warn("Google sign-in rejected: unexpected issuer ({})", issuer);
            return failure("Unexpected issuer: " + issuer);
        }

        // The audience check is the important one. Nimbus validates the signature
        // and expiry but NOT the audience, so without this an ID token issued by
        // Google for a completely different application would be accepted.
        //
        // The claim is read raw rather than through getClaimAsString: Google sends
        // "aud" as a single-element array in this flow, and getClaimAsString
        // stringifies a list into "[value]", which can never equal the configured
        // client id. Both the string and the array form are accepted.
        String audience = audienceOf(jwt);
        String configured = normalize(clientId);
        String received = normalize(audience);
        if (!configured.equals(received)) {
            // A client id is a public identifier, not a secret, so both values are
            // logged in full: comparing them by eye is the fastest way to see a
            // mismatch, and masking them has repeatedly left this undiagnosable.
            log.warn("Google sign-in rejected: audience mismatch. configured=[{}] token=[{}]",
                    configured, received);
            return failure("Token audience does not match this application");
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }

    /**
     * Strips whitespace and any zero-width or BOM characters, and lowercases, so
     * that a value carried through a .env file or a JSON payload still compares
     * equal to the same value written by hand.
     */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        value.codePoints()
                .filter(cp -> !Character.isWhitespace(cp) && cp != 0xFEFF && cp != 0x200B)
                .forEach(sb::appendCodePoint);
        return sb.toString().toLowerCase();
    }

    /**
     * Reads the {@code aud} claim as a plain string.
     *
     * <p>The JWT specification allows {@code aud} to be either a string or an
     * array of strings, and Google uses the array form here. Going through
     * {@code getClaimAsString} in that case yields {@code "[value]"} because the
     * claim is a {@code List} and only its {@code toString} is taken, so a
     * perfectly valid token is rejected as belonging to another application.
     */
    static String audienceOf(Jwt jwt) {
        Object raw = jwt.getClaim(AUDIENCE_CLAIM);
        if (raw instanceof String value) {
            return value;
        }
        if (raw instanceof java.util.Collection<?> values) {
            return values.isEmpty() ? null : String.valueOf(values.iterator().next());
        }
        return raw == null ? null : String.valueOf(raw);
    }
}
