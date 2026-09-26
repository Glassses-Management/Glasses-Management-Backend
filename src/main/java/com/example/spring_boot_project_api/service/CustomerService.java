package com.example.spring_boot_project_api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final CustomerAccessService customerAccessService;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper,
                           UserRepository userRepository, UserMapper userMapper,
                           PasswordEncoder passwordEncoder, JwtService jwtService,
                           CustomerAccessService customerAccessService) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerAccessService = customerAccessService;
    }

    /**
     * The signed-in customer's own profile. Throws EntityNotFoundException (404)
     * when the account has none yet, which is how the frontend detects that a new
     * Google signup still has to complete its profile.
     */
    @Transactional
    public CustomerResponse getMyProfile(Authentication authentication) {
        return customerMapper.toCreatedAtResponse(customerAccessService.requireMyCustomer(authentication));
    }

    /**
     * Creates the customer profile for the signed-in account, or updates it when
     * one is already linked.
     *
     * A Google signup has a name and an email but no phone number, and customer.phone
     * is NOT NULL, so this is where that gap gets filled.
     *
     * The email always comes from the signed-in account and the request's copy is
     * ignored. That is deliberate: it means completing a profile can never collide
     * with a customer row somebody else already owns, and it reuses a staff-created
     * row that matches the account instead of dead-ending on "Email is already in use".
     */
    @Transactional
    public CustomerResponse completeMyProfile(CompleteProfileRequest request, Authentication authentication) {
        User user = customerAccessService.requireUser(authentication);

        // Trim before the uniqueness check: " 5551234 " and "5551234" are the same
        // number, and an untrimmed value would pass the check then fail on the
        // unique index with an opaque constraint violation.
        String phone = request.getPhone() == null ? null : request.getPhone().trim();

        Customer customer = customerAccessService.findMyCustomer(authentication);
        if (customer == null) {
            ensurePhoneAvailable(phone, null);
            customer = new Customer();
        } else {
            ensurePhoneAvailable(phone, customer);
        }

        customer.setName(request.getName().trim());
        customer.setPhone(phone);
        // Always the account's own address, never one from the request body.
        customer.setEmail(user.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDate_of_birth());
        customer.setUser(user);

        return customerMapper.toCreatedAtResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        ensurePhoneAvailable(request.getPhone(), null);
        ensureEmailAvailable(request.getEmail(), null);

        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toCreatedAtResponse(customerRepository.save(customer));
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
