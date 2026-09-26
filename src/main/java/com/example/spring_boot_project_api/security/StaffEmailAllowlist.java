package com.example.spring_boot_project_api.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Opt-in list of staff emails that may sign in with Google.
 *
 * A Google-verified email on its own is not enough to obtain staff access: the
 * address must also appear here. Without this, anyone could create a Google
 * account with a similar-looking address and be granted staff permissions.
 * Staff roles are never granted by the allowlist alone, the user record must
 * already exist with the intended role.
 */
@Component
public class StaffEmailAllowlist {

    private final Set<String> allowed;

    public StaffEmailAllowlist(@Value("${app.google.allowed-staff-emails:}") String configured) {
        this.allowed = parse(configured);
    }

    public boolean isAllowed(String email) {
        return email != null && allowed.contains(normalize(email));
    }

    public boolean isEmpty() {
        return allowed.isEmpty();
    }

    private Set<String> parse(String configured) {
        if (configured == null || configured.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configured.split("[,;\\s]+"))
                .map(StaffEmailAllowlist::normalize)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
