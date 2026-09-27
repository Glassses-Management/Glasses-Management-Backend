package com.example.spring_boot_project_api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CompleteProfileRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRegisterRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.AuthResponse;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.mapper.CustomerMapper;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.JwtService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper,
                           UserRepository userRepository, UserMapper userMapper,
                           PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        ensurePhoneAvailable(request.getPhone(), null);
        ensureEmailAvailable(request.getEmail(), null);

        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toCreatedAtResponse(customerRepository.save(customer));
    }

    /**
     * The signed-in customer's own profile.
     *
     * <p>Self-scoped from the token: the caller is the subject, so there is no id
     * to tamper with. Answers 404 when the account has no customer record yet,
     * which is how a brand new Google signup is detected by the frontend.
     */
    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(Authentication authentication) {
        return customerMapper.toCreatedAtResponse(requireProfileFor(authentication));
    }

    /**
     * Creates the customer record for an account that has none, which is the state
     * a new Google signup starts in: the user row exists, but the customer table
     * requires a phone number that Google does not supply.
     *
     * <p>The email is taken from the verified token rather than the request, and
     * the account is resolved from the token, so this cannot be used to create or
     * overwrite somebody else's profile.
     */
    @Transactional
    public CustomerResponse completeMyProfile(Authentication authentication,
                                              CompleteProfileRequest request) {
        User user = requireUser(authentication);
        String phone = request.getPhone().trim();

        // Checked before the phone, otherwise a second attempt reports "phone
        // already in use" (the account's own number) instead of the real reason.
        if (customerRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalStateException("This account already has a customer profile");
        }

        if (user.getPhone() != null && !user.getPhone().isBlank() && !user.getPhone().equals(phone)) {
            // The account already has a verified number from registration; silently
            // replacing it would let a stolen session take over the contact number.
            throw new IllegalStateException("This account already has a phone number on file");
        }
        ensurePhoneAvailable(phone, null);

        // A customer row may already exist for this address without a linked account
        // (created at the front desk). Claim it rather than failing on the unique
        // email, so the person keeps their history.
        Customer customer = customerRepository.findByEmail(user.getEmail()).orElseGet(Customer::new);
        customer.setName(request.getName().trim());
        customer.setPhone(phone);
        customer.setEmail(user.getEmail());
        customer.setUser(user);
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            customer.setAddress(request.getAddress().trim());
        }
        if (request.getDate_of_birth() != null) {
            customer.setDateOfBirth(request.getDate_of_birth());
        }

        Customer saved = customerRepository.save(customer);

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            user.setPhone(phone);
            userRepository.save(user);
        }

        return customerMapper.toCreatedAtResponse(saved);
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Not authenticated");
        }
        if (authentication.getPrincipal() instanceof Jwt jwt
                && jwt.getClaim("uid") instanceof Number uid) {
            return userRepository.findById(uid.longValue())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Customer requireProfileFor(Authentication authentication) {
        User user = requireUser(authentication);
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));
    }

    @Transactional
    public AuthResponse registerCustomer(CustomerRegisterRequest request) {
        ensurePhoneAvailable(request.getPhone(), null);
        ensureEmailAvailable(request.getEmail(), null);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);
        user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDate_of_birth());
        customer.setUser(savedUser);
        customerRepository.save(customer);

        Instant issuedAt = Instant.now();
        String token = jwtService.generateToken(savedUser);

        AuthResponse response = new AuthResponse();
        response.setAccess_token(token);
        response.setToken_type("Bearer");
        response.setExpires_at(jwtService.expiresAtFrom(issuedAt));
        response.setUser(userMapper.toSummaryResponse(savedUser));

        return response;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream().map(customerMapper::toCreatedAtResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String name, String phone, String email,
                                         LocalDate createdFrom, LocalDate createdTo,
                                         Pageable pageable) {
        assertSortAllowed(pageable, Set.of("id", "name", "createdAt", "updatedAt"));

        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }

        Specification<Customer> spec = (root, query, cb) -> cb.conjunction();

        if (name != null && !name.isBlank()) {
            String like = "%" + name.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (phone != null && !phone.isBlank()) {
            String like = "%" + phone.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("phone")), like));
        }
        if (email != null && !email.isBlank()) {
            String like = "%" + email.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("email")), like));
        }
        if (createdFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay()));
        }
        if (createdTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo.atTime(LocalTime.MAX)));
        }

        return customerRepository.findAll(spec, pageable).map(customerMapper::toCreatedAtResponse);
    }

    private void assertSortAllowed(Pageable pageable, Set<String> allowed) {
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Sorting by '" + order.getProperty() + "' is not allowed");
            }
        });
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return customerMapper.toCreatedAtResponse(getEntity(id));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getEntity(id);

        ensurePhoneAvailable(request.getPhone(), customer);
        ensureEmailAvailable(request.getEmail(), customer);

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDate_of_birth());

        return customerMapper.toUpdatedAtResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.delete(getEntity(id));
    }

    private Customer getEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
    }

    private void ensurePhoneAvailable(String phone, Customer current) {
        if (customerRepository.existsByPhone(phone) && (current == null || !phone.equals(current.getPhone()))) {
            throw new IllegalArgumentException("Phone number is already in use");
        }
    }

    private void ensureEmailAvailable(String email, Customer current) {
        if (customerRepository.existsByEmail(email) && (current == null || !email.equals(current.getEmail()))) {
            throw new IllegalArgumentException("Email is already in use");
        }
    }
}
