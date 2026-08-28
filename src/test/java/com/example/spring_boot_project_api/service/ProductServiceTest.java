package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.dto.response.ProductResponse;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void search_filtersByCategory() {
        Product product = new Product();
        product.setId(1L);
        ProductResponse response = new ProductResponse();
        response.setId(1L);

        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.search("Frame", null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_freeTextMatchesSkuModelBrand() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Product()), pageable, 0));

        productService.search(null, null, null, "ray", pageable);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_rejectsDisallowedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("secret"));

        assertThatThrownBy(() -> productService.search(null, null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void create_throwsWhenSkuExists() {
        ProductRequest request = new ProductRequest();
        request.setSku("SKU1");
        when(productRepository.existsBySku("SKU1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    void getById_throwsWhenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
