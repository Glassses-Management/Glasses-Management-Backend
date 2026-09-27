package com.example.spring_boot_project_api.service;

import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ChangeMyPasswordRequest;
import com.example.spring_boot_project_api.dto.request.GoogleLoginRequest;
import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.GoogleSignInNotPermittedException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.GoogleTokenVerifier;
import com.example.spring_boot_project_api.security.JwtService;
import com.example.spring_boot_project_api.security.StaffEmailAllowlist;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final StaffEmailAllowlist staffEmailAllowlist;

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper,
                       GoogleTokenVerifier googleTokenVerifier,
                       StaffEmailAllowlist staffEmailAllowlist) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.googleTokenVerifier = googleTokenVerifier;
        this.staffEmailAllowlist = staffEmailAllowlist;
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email/phone or password"));

        // Accounts created through Google have no password. matches() would throw
        // on a null hash and surface as a confusing 400, so state the situation
        // plainly. The generic wording keeps the endpoint from revealing which
        // addresses exist.
        if (user.getPassword_hash() == null) {
            throw new BadCredentialsException("This account has no password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword_hash())) {
            throw new BadCredentialsException("Invalid email/phone or password");
        }

        return jwtService.generateToken(user);
    }

    /**
     * Reports whether a Google identity already has an account, without creating
     * one and without issuing a session.
     *
     * <p>Used by the registration page, which must not silently sign somebody into
     * an existing account: that page promises to create an account, so an address
     * that is already registered has to be sent to the sign-in page instead. The
     * token is still fully verified, so the answer cannot be obtained for an
     * address the caller does not control.
     *
     * @return {@code true} when an account already exists for the verified address
     */
    @Transactional(readOnly = true)
    public boolean googleAccountExists(GoogleLoginRequest request) {
        var jwt = googleTokenVerifier.verify(request.getCredential());
        String email = googleTokenVerifier.emailFrom(jwt);
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Signs in with a Google ID token and returns this app's own JWT.
     *
     * The browser only proves who the user is; every authorization decision is made
     * here. Linking is by verified email:
     * <ul>
     *   <li>existing CUSTOMER: signed straight in, password untouched</li>
     *   <li>existing staff: only when the address is on the allowlist</li>
     *   <li>unknown address: a new CUSTOMER with no password</li>
     * </ul>
     * A Google user never has a password_hash, so password login is impossible for
     * them unless a password is set later through /auth/change-password.
     */
    @Transactional
    public String googleLogin(GoogleLoginRequest request) {
        var jwt = googleTokenVerifier.verify(request.getCredential());
        String email = googleTokenVerifier.emailFrom(jwt);
        String name = googleTokenVerifier.nameFrom(jwt);

        User user = userRepository.findByEmail(email)
                .map(existing -> authorizeGoogleStaff(existing, email, name))
                .orElseGet(() -> registerGoogleCustomer(email, name));

        return jwtService.generateToken(user);
    }

    /**
     * A verified Google address is not by itself proof of staff access, so an
     * existing staff account must also be on the allowlist. The stored role is
     * never changed here: the allowlist can only remove access, never grant it.
     */
    private User authorizeGoogleStaff(User existing, String email, String name) {
        if (isStaff(existing.getRole()) && !staffEmailAllowlist.isAllowed(email)) {
            log.warn("Google sign-in refused: staff account is not on the allowlist. "
                    + "Add the address to GOOGLE_ALLOWED_STAFF_EMAILS, or create the "
                    + "account through a different route.");
            throw new GoogleSignInNotPermittedException(
                    "This account is not allowed to sign in with Google. Ask an administrator to add "
                            + email + " to the staff allowlist.");
        }
        applyGoogleName(existing, name);
        return existing;
    }

    /**
     * Brand new Google signups become CUSTOMER. Staff access is deliberately not
     * granted here even for allowlisted addresses, because the allowlist is a
     * guard on existing accounts, not a way to hand out roles.
     *
     * <p>No customer row is created here: the customer table requires a phone
     * number, which Google does not supply. A signup that has no customer record
     * yet is picked up by the frontend through /customers/me returning 404, which
     * sends the browser to the completion page.
     */
    private User registerGoogleCustomer(String email, String name) {
        User user = new User();
        user.setName(name != null ? name : email);
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);
        // No password_hash: this account is reachable through Google only until a
        // password is added from the profile page.
        user.setPassword_hash(null);
        user = userRepository.save(user);

        linkPreExistingCustomer(user, email);
        return user;
    }

    /**
     * Staff can register a customer, and POST /api/customers leaves user_id null.
     * If such a record already exists for this address, claim it now so the
     * person finds their orders and history instead of an empty account.
     */
    private void linkPreExistingCustomer(User user, String email) {
        customerRepository.findByEmail(email)
                .filter(customer -> customer.getUser() == null)
                .ifPresent(customer -> {
                    customer.setUser(user);
                    customerRepository.save(customer);
                });
    }

    /**
     * Reuses the display name Google reports when the account has none yet, so a
     * freshly created customer is not listed as "null" in staff views.
     */
    private void applyGoogleName(User user, String name) {
        if (name != null && (user.getName() == null || user.getName().isBlank())) {
            user.setName(name);
            userRepository.save(user);
        }
    }

    private boolean isStaff(Role role) {
        return role == Role.ADMIN || role == Role.STAFF || role == Role.OPTOMETRIST;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Transactional
    public Map<String, String> changePassword(Authentication authentication, ChangeMyPasswordRequest request) {
        User user = userRepository.findById(userIdFromToken(authentication))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getPassword_hash() == null) {
            // Google accounts start without a password. matches() would throw on a
            // null hash, so report the situation instead of a 500.
            throw new BadCredentialsException(
                    "This account signs in with Google and has no password yet");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword_hash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword_hash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return Map.of("message", "Password changed successfully");
    }

    private Long userIdFromToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt
                && jwt.getClaim("uid") instanceof Number uid) {
            return uid.longValue();
        }
        throw new EntityNotFoundException("User not found");
    }
}
