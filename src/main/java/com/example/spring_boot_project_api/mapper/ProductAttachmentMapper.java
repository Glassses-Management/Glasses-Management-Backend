package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ProductAttachmentResponse;
import com.example.spring_boot_project_api.model.ProductAttachment;

@Component
public class ProductAttachmentMapper {

    public ProductAttachmentResponse toResponse(ProductAttachment productAttachment) {
        if (productAttachment == null) {
            return null;
        }
        ProductAttachmentResponse response = new ProductAttachmentResponse();
        response.setId(productAttachment.getId());
        response.setProductId(productAttachment.getProduct() != null ? productAttachment.getProduct().getId() : null);
        response.setAttachmentId(productAttachment.getAttachment() != null ? productAttachment.getAttachment().getId() : null);
        response.setCreated_at(productAttachment.getCreatedAt());
        return response;
    }
}
