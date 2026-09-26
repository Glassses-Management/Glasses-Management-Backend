package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.spring_boot_project_api.dto.request.GoogleLoginRequest;
import com.example.spring_boot_project_api.exception.GoogleSignInNotPermittedException;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.GoogleTokenVerifier;
import com.example.spring_boot_project_api.security.JwtService;
import com.example.spring_boot_project_api.security.StaffEmailAllowlist;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleLoginTest {

    private static final String EMAIL = "person@example.com";
    private static final String CREDENTIAL = "google-id-token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private com.example.spring_boot_project_api.mapper.UserMapper userMapper;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                customerRepository,
                passwordEncoder,
                jwtService,
                userMapper,
                googleTokenVerifier,
                new StaffEmailAllowlist("boss@example.com"));
    }

    private void givenVerifiedGoogleIdentity() {
        Jwt jwt = Jwt.withTokenValue(CREDENTIAL)
                .header("alg", "RS256")
                .claim("email", EMAIL)
                .claim("email_verified", true)
                .claim("name", "Person Example")
                .build();
        when(googleTokenVerifier.verify(CREDENTIAL)).thenReturn(jwt);
        when(googleTokenVerifier.emailFrom(jwt)).thenReturn(EMAIL);
        when(googleTokenVerifier.nameFrom(jwt)).thenReturn("Person Example");
    }

    private GoogleLoginRequest request() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setCredential(CREDENTIAL);
        return request;
    }

    private User userWithRole(Role role) {
        User user = new User();
        user.setId(7L);
        user.setName("Person Example");
        user.setEmail(EMAIL);
        user.setRole(role);
        return user;
    }

    @Test
    void createsCustomer_whenEmailIsUnknown() {
        givenVerifiedGoogleIdentity();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("app-jwt");

        String token = authService.googleLogin(request());

        assertEquals("app-jwt", token);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();

        assertEquals(Role.CUSTOMER, created.getRole());
        assertEquals(EMAIL, created.getEmail());
        // A Google account has no password, and must not be given a guessable one.
        assertNull(created.getPassword_hash());
    }

    @Test
    void signsInExistingCustomer_withoutTouchingThePassword() {
        givenVerifiedGoogleIdentity();
        User existing = userWithRole(Role.CUSTOMER);
        existing.setPassword_hash("existing-bcrypt-hash");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("app-jwt");

        assertEquals("app-jwt", authService.googleLogin(request()));

        // Linking an existing account must not rewrite its credentials or role.
        verify(userRepository, never()).save(any(User.class));
        assertEquals("existing-bcrypt-hash", existing.getPassword_hash());
        assertEquals(Role.CUSTOMER, existing.getRole());
    }

    @Test
    void refusesStaff_whenEmailIsNotAllowlisted() {
        givenVerifiedGoogleIdentity();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithRole(Role.ADMIN)));

        // A distinct exception, not BadCredentialsException: the caller already
        // proved control of the address through Google, so the handler returns the
        // real reason instead of the generic password-failure message.
        GoogleSignInNotPermittedException thrown = assertThrows(
                GoogleSignInNotPermittedException.class, () -> authService.googleLogin(request()));

        assertTrue(thrown.getMessage().contains(EMAIL), "the message should name the address to add");

        // No token may be issued for a rejected staff account.
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void allowsStaff_whenEmailIsAllowlisted() {
        // Set up explicitly instead of via givenVerifiedGoogleIdentity: this case
        // uses a different address than the shared EMAIL constant.
        Jwt jwt = Jwt.withTokenValue(CREDENTIAL)
                .header("alg", "RS256")
                .claim("email", "boss@example.com")
                .build();
        when(googleTokenVerifier.verify(CREDENTIAL)).thenReturn(jwt);
        when(googleTokenVerifier.emailFrom(jwt)).thenReturn("boss@example.com");
        when(googleTokenVerifier.nameFrom(jwt)).thenReturn("Boss Example");

        User admin = userWithRole(Role.ADMIN);
        admin.setEmail("boss@example.com");
        when(userRepository.findByEmail("boss@example.com")).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(admin)).thenReturn("app-jwt");

        assertEquals("app-jwt", authService.googleLogin(request()));
    }

    @Test
    void neverGrantsStaffRoleFromTheAllowlistAlone() {
        givenVerifiedGoogleIdentity();
        // Allowlisted address, but no user record exists yet. It must become a
        // customer, not an admin.
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("app-jwt");

        authService.googleLogin(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.CUSTOMER, captor.getValue().getRole());
    }

    @Test
    void claimsPreExistingCustomerRecordLeftUnlinkedByStaff() {
        givenVerifiedGoogleIdentity();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        // Staff created this customer through POST /api/customers, so user_id is null.
        Customer orphan = new Customer();
        orphan.setId(3L);
        orphan.setEmail(EMAIL);
        when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(orphan));
        when(customerRepository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("app-jwt");

        authService.googleLogin(request());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer linked = captor.getValue();
        assertNotNull(linked.getUser());
        assertEquals(42L, linked.getUser().getId());
    }

    @Test
    void fillsInMissingDisplayName() {
        givenVerifiedGoogleIdentity();
        User existing = userWithRole(Role.CUSTOMER);
        existing.setName(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtService.generateToken(existing)).thenReturn("app-jwt");

        authService.googleLogin(request());

        assertEquals("Person Example", existing.getName());
    }

    @Test
    void propagatesVerificationFailure() {
        when(googleTokenVerifier.verify(anyString()))
                .thenThrow(new BadCredentialsException("Google token is invalid or expired"));

        // An unverified token must never reach the account lookup.
        assertThrows(BadCredentialsException.class, () -> authService.googleLogin(request()));
        verify(userRepository, never()).findByEmail(anyString());
    }
}
