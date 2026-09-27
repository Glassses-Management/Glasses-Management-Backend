package com.example.spring_boot_project_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The allowlist can only remove access from an existing staff account, so the
 * cases that matter are an unlisted address, case and whitespace handling, and
 * the empty configuration that would otherwise lock every staff member out.
 */
class StaffEmailAllowlistTest {

    @Test
    void anEmptyConfigurationAllowsNobody() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("");

        assertThat(allowlist.isAllowed("anyone@example.com")).isFalse();
        assertThat(allowlist.size()).isZero();
    }

    @Test
    void aListedAddressIsAllowed() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("boss@example.com");

        assertThat(allowlist.isAllowed("boss@example.com")).isTrue();
    }

    @Test
    void anUnlistedAddressIsNotAllowed() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("boss@example.com");

        assertThat(allowlist.isAllowed("someone.else@example.com")).isFalse();
    }

    @Test
    void severalAddressesAreSeparatedByCommas() {
        StaffEmailAllowlist allowlist =
                new StaffEmailAllowlist("boss@example.com, optometrist@example.com");

        assertThat(allowlist.size()).isEqualTo(2);
        assertThat(allowlist.isAllowed("optometrist@example.com")).isTrue();
    }

    @Test
    void matchingIgnoresCaseAndSurroundingWhitespace() {
        // Configuration is written by hand in a .env file, so stray spaces and a
        // capitalised address must not silently exclude someone.
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist(" Boss@Example.com , other@example.com ");

        assertThat(allowlist.isAllowed("boss@example.com")).isTrue();
        assertThat(allowlist.isAllowed("BOSS@EXAMPLE.COM")).isTrue();
    }

    @Test
    void duplicateEntriesCollapse() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("boss@example.com,boss@example.com");

        assertThat(allowlist.size()).isEqualTo(1);
    }

    @Test
    void aNullAddressIsNeverAllowed() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("boss@example.com");

        assertThat(allowlist.isAllowed(null)).isFalse();
    }
}
