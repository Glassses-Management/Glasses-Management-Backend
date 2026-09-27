package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.spring_boot_project_api.dto.request.GoogleLoginRequest;
import com.example.spring_boot_project_api.exception.GoogleSignInNotPermittedException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.GoogleTokenVerifier;
import com.example.spring_boot_project_api.security.JwtService;
import com.example.spring_boot_project_api.security.StaffEmailAllowlist;

/**
 * Google sign-in runs on its own path: no password is read, checked or written,
 * and the role always comes from the stored account rather than from Google or
 * from configuration. These tests cover the decisions that could leak access.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceGoogleLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private StaffEmailAllowlist staffEmailAllowlist;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, customerRepository, passwordEncoder,
                jwtService, userMapper, googleTokenVerifier, staffEmailAllowlist);
    }

    private static GoogleLoginRequest request() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setCredential("header.payload.signature");
        return request;
    }

    private void tokenFor(String email) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").claim("email", email).build();
        when(googleTokenVerifier.verify(anyString())).thenReturn(jwt);
        when(googleTokenVerifier.emailFrom(jwt)).thenReturn(email);
    }

    @Test
    void anUnknownAddressBecomesACustomerWithNoPassword() {
        tokenFor("new@example.com");
        when(googleTokenVerifier.nameFrom(any())).thenReturn("New Person");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("issued-token");

        assertThat(authService.googleLogin(request())).isEqualTo("issued-token");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        // No password may be invented for a Google account.
        assertThat(saved.getValue().getPassword_hash()).isNull();
    }

    @Test
    void anAllowlistedAddressNeverBecomesStaff() {
        // The allowlist guards existing accounts; it must not be able to hand out a
        // role to a brand new one.
        tokenFor("boss@example.com");
        when(googleTokenVerifier.nameFrom(any())).thenReturn("Boss");
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.empty());
        when(staffEmailAllowlist.isAllowed("boss@example.com")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        authService.googleLogin(request());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void anExistingCustomerSignsInWithoutTheirPasswordBeingTouched() {
        tokenFor("customer@example.com");
        User existing = new User();
        existing.setId(5L);
        existing.setName("Customer");
        existing.setEmail("customer@example.com");
        existing.setRole(Role.CUSTOMER);
        existing.setPassword_hash("existing-bcrypt-hash");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("issued-token");

        assertThat(authService.googleLogin(request())).isEqualTo("issued-token");

        assertThat(existing.getPassword_hash())
                .as("a password account must keep its password when using Google")
                .isEqualTo("existing-bcrypt-hash");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void aStaffAccountNotOnTheAllowlistIsRefused() {
        tokenFor("boss@example.com");
        User boss = new User();
        boss.setId(9L);
        boss.setEmail("boss@example.com");
        boss.setRole(Role.ADMIN);
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.of(boss));
        when(staffEmailAllowlist.isAllowed("boss@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.googleLogin(request()))
                .isInstanceOf(GoogleSignInNotPermittedException.class);
    }

    @Test
    void anAllowlistedStaffAccountSignsIn() {
        tokenFor("boss@example.com");
        User boss = new User();
        boss.setId(9L);
        boss.setName("Boss");
        boss.setEmail("boss@example.com");
        boss.setRole(Role.ADMIN);
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.of(boss));
        when(staffEmailAllowlist.isAllowed("boss@example.com")).thenReturn(true);
        when(jwtService.generateToken(boss)).thenReturn("issued-token");

        assertThat(authService.googleLogin(request())).isEqualTo("issued-token");
    }

    @Test
    void aRejectedTokenNeverReachesTheDatabase() {
        when(googleTokenVerifier.verify(anyString()))
                .thenThrow(new com.example.spring_boot_project_api.exception.GoogleSignInFailedException(
                        "The Google token could not be verified."));

        assertThatThrownBy(() -> authService.googleLogin(request()))
                .isInstanceOf(com.example.spring_boot_project_api.exception.GoogleSignInFailedException.class);

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void theExistenceCheckReportsWithoutCreatingAnything() {
        tokenFor("known@example.com");
        when(userRepository.findByEmail("known@example.com")).thenReturn(Optional.of(new User()));

        assertThat(authService.googleAccountExists(request())).isTrue();
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void theExistenceCheckReportsFalseForANewAddress() {
        tokenFor("new@example.com");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThat(authService.googleAccountExists(request())).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }
}
