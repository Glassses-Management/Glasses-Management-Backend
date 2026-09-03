package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.ProductAttachmentResponse;
import com.example.spring_boot_project_api.service.ProductAttachmentService;

@RestController
@RequestMapping("/api")
public class ProductAttachmentController {

    private final ProductAttachmentService productAttachmentService;

    public ProductAttachmentController(ProductAttachmentService productAttachmentService) {
        this.productAttachmentService = productAttachmentService;
    }

    @PostMapping("/products/{productId}/attachments/{attachmentId}")
    public ResponseEntity<ProductAttachmentResponse> create(@PathVariable Long productId, @PathVariable Long attachmentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productAttachmentService.create(productId, attachmentId));
    }

    @GetMapping("/products/{productId}/attachments")
    public List<ProductAttachmentResponse> getByProductId(@PathVariable Long productId) {
        return productAttachmentService.getByProductId(productId);
    }

    @GetMapping("/product-attachments/{id}")
    public ProductAttachmentResponse getById(@PathVariable Long id) {
        return productAttachmentService.getById(id);
    }

    @DeleteMapping("/product-attachments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productAttachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
