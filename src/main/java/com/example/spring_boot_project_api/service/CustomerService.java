package com.example.spring_boot_project_api.service;

import java.util.List;

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
