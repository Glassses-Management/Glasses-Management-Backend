package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.ProductAttachmentResponse;
import com.example.spring_boot_project_api.mapper.ProductAttachmentMapper;
import com.example.spring_boot_project_api.model.Attachment;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductAttachment;
import com.example.spring_boot_project_api.repository.AttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductAttachmentService {

    private final ProductAttachmentRepository productAttachmentRepository;
    private final ProductAttachmentMapper productAttachmentMapper;
    private final ProductRepository productRepository;
    private final AttachmentRepository attachmentRepository;

    public ProductAttachmentService(ProductAttachmentRepository productAttachmentRepository,
                                    ProductAttachmentMapper productAttachmentMapper,
                                    ProductRepository productRepository,
                                    AttachmentRepository attachmentRepository) {
        this.productAttachmentRepository = productAttachmentRepository;
        this.productAttachmentMapper = productAttachmentMapper;
        this.productRepository = productRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public ProductAttachmentResponse create(Long productId, Long attachmentId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));

        ProductAttachment productAttachment = new ProductAttachment();
        productAttachment.setProduct(product);
        productAttachment.setAttachment(attachment);

        return productAttachmentMapper.toResponse(productAttachmentRepository.save(productAttachment));
    }

    @Transactional(readOnly = true)
    public List<ProductAttachmentResponse> getByProductId(Long productId) {
        return productAttachmentRepository.findByProductId(productId).stream()
                .map(productAttachmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductAttachmentResponse getById(Long id) {
        return productAttachmentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public void delete(Long id) {
        productAttachmentRepository.delete(getEntity(id));
    }

    private ProductAttachment getEntity(Long id) {
        return productAttachmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProductAttachment not found"));
    }
}
