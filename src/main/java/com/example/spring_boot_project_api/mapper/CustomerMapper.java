package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.model.Customer;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDate_of_birth());
        return customer;
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setAddress(customer.getAddress());
        response.setDate_of_birth(customer.getDateOfBirth());
        response.setCreated_at(customer.getCreatedAt());
        return response;
    }
}
