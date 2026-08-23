package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.dto.response.ProductResponse;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper){
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request){
        if (productRepository.existsBySku(request.getSku())) {
            throw new EntityExistsException("SKU already exists");
        }

        Product product = productMapper.toEntity(request);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(){
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id){
        return productMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request){
        Product product = getEntity(id);

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new EntityExistsException("SKU already exists");
        }

        product.setSku(request.getSku());
        product.setModel(request.getModel());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setMaterial(request.getMaterial());
        product.setSize(request.getSize());
        product.setCost_price(request.getCost_price());
        product.setSale_price(request.getSale_price());
        product.setSupplier_name(request.getSupplier_name());
        product.setSupplier_contact(request.getSupplier_contact());
        product.setCategory(request.getCategory());

        return productMapper.toResponse(productRepository.save(product));
    }

    public void delete(Long id){
        productRepository.delete(getEntity(id));
    }

    private Product getEntity(Long id){
        return productRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }
}
