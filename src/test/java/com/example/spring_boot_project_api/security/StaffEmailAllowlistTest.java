package com.example.spring_boot_project_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StaffEmailAllowlistTest {

    @Test
    void isEmpty_whenNothingConfigured() {
        assertThat(new StaffEmailAllowlist("").isEmpty()).isTrue();
        assertThat(new StaffEmailAllowlist(null).isEmpty()).isTrue();
        assertThat(new StaffEmailAllowlist("   ").isEmpty()).isTrue();
    }

    @Test
    void rejectsEveryone_whenEmpty() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("");

        assertThat(allowlist.isAllowed("admin@example.com")).isFalse();
    }

    @Test
    void allowsListedEmail() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("admin@example.com,optometrist@example.com");

        assertThat(allowlist.isAllowed("admin@example.com")).isTrue();
        assertThat(allowlist.isAllowed("optometrist@example.com")).isTrue();
    }

    @Test
    void rejectsUnlistedEmail() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("admin@example.com");

        assertThat(allowlist.isAllowed("someone.else@example.com")).isFalse();
        assertThat(allowlist.isAllowed(null)).isFalse();
    }

    @Test
    void matchesRegardlessOfCaseAndPadding() {
        // Addresses are normalised on both sides, so a mixed-case entry in the
        // env file still matches the lowercase email stored on the account.
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist(" Admin@Example.COM , second@example.com ");

        assertThat(allowlist.isAllowed("admin@example.com")).isTrue();
        assertThat(allowlist.isAllowed("ADMIN@EXAMPLE.COM")).isTrue();
        assertThat(allowlist.isAllowed("second@example.com")).isTrue();
    }

    @Test
    void acceptsSeparatorsOtherThanComma() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("a@example.com; b@example.com\nc@example.com");

        assertThat(allowlist.isAllowed("a@example.com")).isTrue();
        assertThat(allowlist.isAllowed("b@example.com")).isTrue();
        assertThat(allowlist.isAllowed("c@example.com")).isTrue();
    }
}
