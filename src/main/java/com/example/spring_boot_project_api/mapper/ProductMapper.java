package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.dto.response.ProductResponse;
import com.example.spring_boot_project_api.model.Product;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest dto) {
        Product product = new Product();

        product.setSku(dto.getSku());
        product.setModel(dto.getModel());
        product.setBrand(dto.getBrand());
        product.setColor(dto.getColor());
        product.setMaterial(dto.getMaterial());
        product.setSize(dto.getSize());
        product.setCost_price(dto.getCost_price());
        product.setSale_price(dto.getSale_price());
        product.setSupplier_name(dto.getSupplier_name());
        product.setSupplier_contact(dto.getSupplier_contact());
        product.setCategory(dto.getCategory());

        return product;
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setModel(product.getModel());
        response.setBrand(product.getBrand());
        response.setColor(product.getColor());
        response.setMaterial(product.getMaterial());
        response.setSize(product.getSize());
        response.setCost_price(product.getCost_price());
        response.setSale_price(product.getSale_price());
        response.setSupplier_name(product.getSupplier_name());
        response.setSupplier_contact(product.getSupplier_contact());
        response.setCategory(product.getCategory());
        response.setCreated_at(product.getCreatedAt());
        response.setUpdated_at(product.getUpdatedAt());

        return response;
    }

    public ProductResponse toCreatedAtResponse(Product product) {
        ProductResponse response = toResponse(product);
        if (response == null) {
            return null;
        }
        response.setUpdated_at(null);
        return response;
    }

    public ProductResponse toUpdatedAtResponse(Product product) {
        ProductResponse response = toResponse(product);
        if (response == null) {
            return null;
        }
        response.setCreated_at(null);
        return response;
    }
}
