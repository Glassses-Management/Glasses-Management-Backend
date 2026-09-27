package com.example.spring_boot_project_api.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Addresses permitted to sign in with Google using a staff role.
 *
 * <p>This can only remove access, never grant it. An address that is not on the
 * list can still sign in if it belongs to a customer account; the list merely
 * stops a staff account from authenticating through Google until an
 * administrator adds it. Roles are never assigned from configuration.
 */
@Component
public class StaffEmailAllowlist {

    private final Set<String> allowed;

    public StaffEmailAllowlist(@Value("${app.google.allowed-staff-emails:}") String configured) {
        this.allowed = parse(configured);
        if (this.allowed.isEmpty()) {
            // Loud on purpose: an empty list means no staff account can use Google,
            // which otherwise looks like a broken button.
            org.slf4j.LoggerFactory.getLogger(StaffEmailAllowlist.class)
                    .warn("No Google staff allowlist configured. Staff accounts cannot sign in with "
                            + "Google until GOOGLE_ALLOWED_STAFF_EMAILS is set.");
        }
    }

    static Set<String> parse(String configured) {
        if (configured == null || configured.isBlank()) {
            return Set.of();
        }
        // LinkedHashSet so duplicate entries collapse instead of accumulating.
        return new LinkedHashSet<>(Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList());
    }

    public boolean isAllowed(String email) {
        return email != null && allowed.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public int size() {
        return allowed.size();
    }
}
