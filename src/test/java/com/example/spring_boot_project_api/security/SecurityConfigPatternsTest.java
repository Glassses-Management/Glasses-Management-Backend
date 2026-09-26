package com.example.spring_boot_project_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PatternParseException;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Guards the request matchers declared in {@link SecurityConfig}.
 *
 * A pattern such as "/api/users/**&#47;attachments/**" compiles without complaint
 * and is only rejected when the filter chain is built, so a bad pattern reaches
 * the log as an "Invalid mapping pattern detected" startup crash instead of a
 * failing build. Parsing every pattern here turns that into a failing test.
 */
class SecurityConfigPatternsTest {

    private static Stream<String> allPatterns() {
        List<String> patterns = new ArrayList<>();
        addAll(patterns, SecurityConfig.PUBLIC_PATHS);
        addAll(patterns, SecurityConfig.PUBLIC_GET_PATHS);
        addAll(patterns, SecurityConfig.SELF_PATHS);
        addAll(patterns, SecurityConfig.ACCOUNT_PATHS);
        addAll(patterns, SecurityConfig.CUSTOMER_WRITABLE_PATHS);
        addAll(patterns, SecurityConfig.ATTACHMENT_STAFF_PATHS);
        addAll(patterns, SecurityConfig.STAFF_RECORD_PATHS);
        return patterns.stream();
    }

    private static void addAll(List<String> target, String[] patterns) {
        for (String pattern : patterns) {
            target.add(pattern);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allPatterns")
    void patternIsAcceptedByThePathPatternParser(String pattern) {
        // Same parser Spring Security uses to build the matchers, so anything it
        // rejects here would crash the application at startup.
        assertThatCode(() -> new PathPatternParser().parse(pattern))
                .doesNotThrowAnyException();
    }

    @Test
    void theParserActuallyRejectsAWildcardInTheMiddle() {
        // Guards the test above: if this ever stopped throwing, the whole class
        // would pass no matter what the configuration contained.
        assertThrows(PatternParseException.class,
                () -> new PathPatternParser().parse("/api/users/**/attachments/**"));
    }

    @Test
    void theFixedUserAttachmentPatternsMatchTheRealEndpoints() {
        var parser = new PathPatternParser();
        // GET /api/users/{userId}/attachments and
        // POST /api/users/{userId}/attachments/{attachmentId}
        var list = parser.parse("/api/users/*/attachments");
        var item = parser.parse("/api/users/*/attachments/*");

        assertThat(list.matches(PathContainer.parsePath("/api/users/7/attachments"))).isTrue();
        assertThat(item.matches(PathContainer.parsePath("/api/users/7/attachments/3"))).isTrue();
    }
}
