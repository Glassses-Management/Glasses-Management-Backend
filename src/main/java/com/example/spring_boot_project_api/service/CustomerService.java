package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.mapper.CustomerMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.repository.CustomerRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        ensurePhoneAvailable(request.getPhone(), null);
        ensureEmailAvailable(request.getEmail(), null);

        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toCreatedAtResponse(customerRepository.save(customer));
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
