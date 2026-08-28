package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.mapper.CustomerMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void search_returnsPagedResults() {
        Customer customer = new Customer();
        customer.setId(1L);
        CustomerResponse response = new CustomerResponse();
        response.setId(1L);

        Pageable pageable = PageRequest.of(0, 20);
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toCreatedAtResponse(customer)).thenReturn(response);

        Page<CustomerResponse> result = customerService.search(null, "123", null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(customerRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_rejectsDisallowedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("password"));

        assertThatThrownBy(() -> customerService.search(null, null, null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void search_rejectsInvalidDateRange() {
        assertThatThrownBy(() -> customerService.search(null, null, null,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1), PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdFrom must not be after createdTo");
    }

    @Test
    void create_throwsWhenPhoneAlreadyInUse() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane");
        request.setPhone("0912345678");
        request.setEmail("jane@example.com");
        when(customerRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void create_savesNewCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Jane");
        request.setPhone("0912345678");
        request.setEmail("jane@example.com");

        Customer saved = new Customer();
        saved.setId(2L);
        CustomerResponse response = new CustomerResponse();
        response.setId(2L);

        when(customerRepository.existsByPhone("0912345678")).thenReturn(false);
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(saved);
        when(customerRepository.save(saved)).thenReturn(saved);
        when(customerMapper.toCreatedAtResponse(saved)).thenReturn(response);

        assertThat(customerService.create(request).getId()).isEqualTo(2L);
    }
}
