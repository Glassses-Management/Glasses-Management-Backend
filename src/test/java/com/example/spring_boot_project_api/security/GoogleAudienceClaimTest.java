package com.example.spring_boot_project_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The audience claim decides which application a token was minted for, and it is
 * the check that stops a token from another Google client being accepted.
 *
 * <p>Google sends "aud" as a single-element array. getClaimAsString would take
 * only that list's toString, producing "[value]", which never equals the
 * configured client id - so every valid token was rejected as belonging to another
 * application. These tests pin both accepted shapes.
 */
class GoogleAudienceClaimTest {

    private static final String CLIENT_ID = "181857246153-example.apps.googleusercontent.com";

    private static Jwt jwtWithAudience(Object audience) {
        return new Jwt("token-value",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("aud", audience, "email", "user@example.com"));
    }

    @Test
    void audienceIsReadWhenGoogleSendsASingleElementArray() {
        assertThat(GoogleTokenVerifier.audienceOf(jwtWithAudience(List.of(CLIENT_ID))))
                .isEqualTo(CLIENT_ID);
    }

    @Test
    void audienceIsReadWhenItIsAPlainString() {
        assertThat(GoogleTokenVerifier.audienceOf(jwtWithAudience(CLIENT_ID)))
                .isEqualTo(CLIENT_ID);
    }

    @Test
    void theArrayFormIsNotLeftAsAStringifiedList() {
        // This is the value the old code compared against, and the whole reason
        // sign-in failed: "[value]" instead of "value".
        assertThat(jwtWithAudience(List.of(CLIENT_ID)).getClaimAsString("aud"))
                .isNotEqualTo(CLIENT_ID)
                .startsWith("[");

        assertThat(GoogleTokenVerifier.audienceOf(jwtWithAudience(List.of(CLIENT_ID))))
                .as("audienceOf must unwrap the list so the comparison can succeed")
                .isEqualTo(CLIENT_ID);
    }

    @Test
    void firstEntryWinsWhenSeveralAudiencesArePresent() {
        assertThat(GoogleTokenVerifier.audienceOf(
                jwtWithAudience(List.of(CLIENT_ID, "other-client.apps.googleusercontent.com"))))
                .isEqualTo(CLIENT_ID);
    }

    @Test
    void missingAudienceIsReportedAsAbsent() {
        assertThat(GoogleTokenVerifier.audienceOf(jwtWithAudience(List.of()))).isNull();
    }

    @Test
    void normalisingRemovesInvisibleCharactersFromAConfiguredId() {
        // A client id read from a CRLF .env file picks up a trailing carriage
        // return, which would otherwise never compare equal.
        assertThat(GoogleTokenVerifier.normalize(CLIENT_ID + "\r\n")).isEqualTo(CLIENT_ID);
    }

    @Test
    void normalisingIsCaseInsensitiveSoAMisTypedValueStillMatches() {
        assertThat(GoogleTokenVerifier.normalize(CLIENT_ID.toUpperCase()))
                .isEqualTo(GoogleTokenVerifier.normalize(CLIENT_ID));
    }
}
