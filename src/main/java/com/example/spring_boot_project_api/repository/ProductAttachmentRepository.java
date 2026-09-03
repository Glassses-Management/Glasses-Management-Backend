package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.ProductAttachment;

public interface ProductAttachmentRepository extends JpaRepository<ProductAttachment, Long>{

    List<ProductAttachment> findByProductId(Long productId);

    List<ProductAttachment> findByAttachmentId(Long attachmentId);

    void deleteByAttachmentId(Long attachmentId);
}