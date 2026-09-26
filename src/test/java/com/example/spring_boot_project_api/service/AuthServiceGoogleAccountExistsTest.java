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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.spring_boot_project_api.dto.request.GoogleLoginRequest;
import com.example.spring_boot_project_api.exception.GoogleSignInFailedException;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.GoogleTokenVerifier;
import com.example.spring_boot_project_api.security.JwtService;
import com.example.spring_boot_project_api.security.StaffEmailAllowlist;

/**
 * The registration page uses googleAccountExists to refuse an address that is
 * already registered, instead of silently signing the visitor into the existing
 * account. These tests pin the two properties that matter for that decision: the
 * answer reflects reality, and asking the question never changes anything.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceGoogleAccountExistsTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private com.example.spring_boot_project_api.mapper.UserMapper userMapper;
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
        request.setCredential("a.b.c");
        return request;
    }

    private void tokenFor(String email) {
        when(googleTokenVerifier.verify(anyString())).thenReturn(Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("email", email)
                .build());
        when(googleTokenVerifier.emailFrom(any())).thenReturn(email);
    }

    @Test
    void reportsTrueWhenAnAccountAlreadyExists() {
        tokenFor("known@example.com");
        when(userRepository.findByEmail("known@example.com"))
                .thenReturn(Optional.of(existingUser("known@example.com")));

        assertThat(authService.googleAccountExists(request())).isTrue();
    }

    @Test
    void reportsFalseForAnAddressThatIsNotRegistered() {
        tokenFor("new@example.com");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        assertThat(authService.googleAccountExists(request())).isFalse();
    }

    @Test
    void askingCreatesNoAccountAndIssuesNoToken() {
        tokenFor("new@example.com");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        authService.googleAccountExists(request());

        // The whole point is that the registration page can ask without acting.
        verify(userRepository, never()).save(any(User.class));
        verify(customerRepository, never()).save(any(Customer.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void aRejectedTokenIsNeverTreatedAsAFreshAccount() {
        // Without this, an invalid credential would answer "false" and let the
        // registration page continue as if the address were new.
        when(googleTokenVerifier.verify(anyString()))
                .thenThrow(new GoogleSignInFailedException("The Google token could not be verified."));

        assertThatThrownBy(() -> authService.googleAccountExists(request()))
                .isInstanceOf(GoogleSignInFailedException.class);
    }

    private static User existingUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
