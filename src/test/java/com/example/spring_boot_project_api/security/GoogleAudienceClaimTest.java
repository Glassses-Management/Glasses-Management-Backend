package com.example.spring_boot_project_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The audience check reads the raw claim rather than using getClaimAsString.
 *
 * <p>Google sends {@code aud} as a single-element array. getClaimAsString turns
 * a List into {@code "[value]"}, so the comparison against the configured client
 * id always failed and every valid token was rejected as belonging to another
 * application. These tests pin both accepted shapes so that cannot regress.
 */
class GoogleAudienceClaimTest {

    private static final String CLIENT_ID = "181857246153-example.apps.googleusercontent.com";

    private static Jwt jwtWithAudience(Object audience) {
        Instant now = Instant.now();
        return new Jwt("token-value", now, now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("aud", audience, "email", "user@example.com"));
    }

    @Test
    void audienceIsReadWhenGoogleSendsASingleElementArray() {
        Jwt jwt = jwtWithAudience(List.of(CLIENT_ID));

        assertThat(GoogleTokenVerifier.audienceOf(jwt)).isEqualTo(CLIENT_ID);
    }

    @Test
    void audienceIsReadWhenItIsAPlainString() {
        Jwt jwt = jwtWithAudience(CLIENT_ID);

        assertThat(GoogleTokenVerifier.audienceOf(jwt)).isEqualTo(CLIENT_ID);
    }

    @Test
    void theArrayFormIsNotLeftAsAStringifiedList() {
        Jwt jwt = jwtWithAudience(List.of(CLIENT_ID));

        // This is exactly the value that used to be compared, and the reason
        // every sign-in failed: "[value]" instead of "value".
        assertThat(jwt.getClaimAsString("aud"))
                .isNotEqualTo(CLIENT_ID)
                .startsWith("[");

        assertThat(GoogleTokenVerifier.audienceOf(jwt))
                .as("audienceOf must unwrap the list so the comparison can succeed")
                .isEqualTo(CLIENT_ID);
    }

    @Test
    void firstEntryWinsWhenSeveralAudiencesArePresent() {
        Jwt jwt = jwtWithAudience(List.of(CLIENT_ID, "other-client.apps.googleusercontent.com"));

        assertThat(GoogleTokenVerifier.audienceOf(jwt)).isEqualTo(CLIENT_ID);
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
}
