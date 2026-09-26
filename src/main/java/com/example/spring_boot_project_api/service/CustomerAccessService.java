package com.example.spring_boot_project_api.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Resolves the account and customer profile behind the current JWT.
 *
 * Every "mine" endpoint goes through here so that ownership is always taken from
 * the token, never from a request parameter. Passing a customerId in the query
 * string would let any signed-in user read another customer's records.
 */
@Service
public class CustomerAccessService {

    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_STAFF", "ROLE_OPTOMETRIST");

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomerAccessService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * The signed-in account, read from the "uid" claim that JwtService writes.
     */
    @Transactional(readOnly = true)
    public User requireUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Jwt jwt)
                || !(jwt.getClaim("uid") instanceof Number uid)) {
            throw new EntityNotFoundException("User not found");
        }
        return userRepository.findById(uid.longValue())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    /**
     * The customer profile belonging to the caller, or null when the account has
     * none yet (the normal state for a brand new Google signup, which cannot
     * supply the phone number the customer table requires).
     *
     * Profiles created by staff through POST /api/customers have user_id = NULL,
     * because CustomerMapper never sets it. Those rows are matched on email and
     * the missing link is written on the spot, so such an account is never locked
     * out of its own history.
     */
    @Transactional
    public Customer findMyCustomer(Authentication authentication) {
        return resolve(authentication).orElse(null);
    }

    @Transactional
    public Customer requireMyCustomer(Authentication authentication) {
        return resolve(authentication)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No customer profile is linked to this account"));
    }

    /**
     * True when the caller holds any staff role. Used where one endpoint serves
     * both staff and customers and the owner has to be resolved differently.
     */
    public boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> STAFF_ROLES.contains(authority));
    }

    private Optional<Customer> resolve(Authentication authentication) {
        User user = requireUser(authentication);
        return customerRepository.findByUserId(user.getId())
                .or(() -> linkUnlinkedProfileByEmail(user));
    }

    private Optional<Customer> linkUnlinkedProfileByEmail(User user) {
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return customerRepository.findByEmail(email.trim().toLowerCase())
                .map(customer -> {
                    if (customer.getUser() == null) {
                        customer.setUser(user);
                        return customerRepository.save(customer);
                    }
                    return customer;
                });
    }
}
