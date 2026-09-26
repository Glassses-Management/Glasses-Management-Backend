package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CompleteProfileRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRegisterRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.AuthResponse;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.registerCustomer(request));
    }

    /**
     * The signed-in customer's own profile. 404 when the account has no profile
     * yet, which tells the frontend to send a new Google signup to the
     * complete-profile page.
     */
    @GetMapping("/me")
    public CustomerResponse me(Authentication authentication) {
        return customerService.getMyProfile(authentication);
    }

    /**
     * Creates or updates the signed-in customer's profile. Used by the Google
     * signup flow, which has no phone number to send with the registration form.
     * The email comes from the token, not the body.
     */
    @PostMapping("/complete-profile")
    public ResponseEntity<CustomerResponse> completeProfile(Authentication authentication,
                                                            @Valid @RequestBody CompleteProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.completeMyProfile(request, authentication));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<CustomerResponse>> createBulk(@Valid @RequestBody List<@Valid CustomerRequest> request){
        List<CustomerResponse> saved = request.stream().map(customerService::create).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public Page<CustomerResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            Pageable pageable) {
        return customerService.search(name, phone, email, createdFrom, createdTo, pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
