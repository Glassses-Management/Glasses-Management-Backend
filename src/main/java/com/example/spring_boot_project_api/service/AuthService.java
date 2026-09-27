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

        // A Google account has no password_hash. PasswordEncoder.matches throws on a
        // null hash, and the account genuinely has no password to match, so it is
        // refused here with the same message as any other bad password rather than
        // surfacing as a server error.
        if (user.getPassword_hash() == null || user.getPassword_hash().isBlank()
                || !passwordEncoder.matches(request.getPassword(), user.getPassword_hash())) {
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
     * that is already registered is sent to the sign-in page instead. The token is
     * still fully verified, so the answer cannot be obtained for an address the
     * caller does not control.
     */
    @Transactional(readOnly = true)
    public boolean googleAccountExists(GoogleLoginRequest request) {
        Jwt jwt = googleTokenVerifier.verify(request.getCredential());
        return userRepository.findByEmail(googleTokenVerifier.emailFrom(jwt)).isPresent();
    }

    /**
     * Signs in with a Google ID token and returns this app's own JWT.
     *
     * <p>Deliberately a separate path from {@link #login}: no password is read,
     * checked or set here. The browser only proves who the user is; every
     * authorization decision is made here. Linking is by verified email:
     * <ul>
     *   <li>existing CUSTOMER: signed straight in, any existing password untouched</li>
     *   <li>existing staff: only when the address is on the allowlist</li>
     *   <li>unknown address: a new CUSTOMER with no password</li>
     * </ul>
     *
     * <p>A Google user has no password_hash, so password login is impossible for
     * them until one is set later.
     */
    @Transactional
    public String googleLogin(GoogleLoginRequest request) {
        Jwt jwt = googleTokenVerifier.verify(request.getCredential());
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
            log.warn("Google sign-in refused: staff account is not on the allowlist. Add the address "
                    + "to GOOGLE_ALLOWED_STAFF_EMAILS, or create the account another way.");
            throw new GoogleSignInNotPermittedException(
                    "This account is not allowed to sign in with Google. Ask an administrator to add "
                            + email + " to the staff allowlist.");
        }
        if (existing.getName() == null || existing.getName().isBlank()) {
            // Google knows the display name, so an account created before this
            // field was filled in can be completed on first sign-in.
            existing.setName(name != null ? name : email);
            userRepository.save(existing);
        }
        return existing;
    }

    /**
     * Brand new Google signups become CUSTOMER. Staff access is deliberately not
     * granted even for an allowlisted address, because the allowlist guards
     * existing accounts and is not a way to hand out roles.
     *
     * <p>No customer record is created: the customer table requires a phone
     * number, which Google does not supply. The frontend detects the missing
     * profile and collects it on the completion page.
     */
    private User registerGoogleCustomer(String email, String name) {
        User user = new User();
        user.setName(name != null ? name : email);
        user.setEmail(email);
        user.setRole(Role.CUSTOMER);
        // No password_hash: reachable through Google only until a password is set.
        user.setPassword_hash(null);
        return userRepository.save(user);
    }

    private static boolean isStaff(Role role) {
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
