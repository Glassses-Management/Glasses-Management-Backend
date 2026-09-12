package com.example.spring_boot_project_api.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.AttachmentRequest;
import com.example.spring_boot_project_api.dto.response.AttachmentResponse;
import com.example.spring_boot_project_api.mapper.AttachmentMapper;
import com.example.spring_boot_project_api.model.Attachment;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductAttachment;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserAttachment;
import com.example.spring_boot_project_api.repository.AttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserAttachmentRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserAttachmentRepository userAttachmentRepository;
    private final ProductAttachmentRepository productAttachmentRepository;

    public AttachmentService(AttachmentRepository attachmentRepository, AttachmentMapper attachmentMapper,
            CloudinaryStorageService cloudinaryStorageService, UserRepository userRepository,
            ProductRepository productRepository, UserAttachmentRepository userAttachmentRepository,
            ProductAttachmentRepository productAttachmentRepository) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentMapper = attachmentMapper;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.userAttachmentRepository = userAttachmentRepository;
        this.productAttachmentRepository = productAttachmentRepository;
    }

    @Transactional
    public AttachmentResponse create(AttachmentRequest request) {
        Attachment attachment = attachmentMapper.toEntity(request);
        return attachmentMapper.toCreatedAtResponse(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAll() {
        return attachmentRepository.findAll().stream().map(attachmentMapper::toCreatedAtResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<AttachmentResponse> search(String fileName, String fileType, Pageable pageable) {
        assertSortAllowed(pageable, Set.of("id", "fileName", "fileType", "fileSize", "createdAt", "updatedAt"));

        Specification<Attachment> spec = (root, query, cb) -> cb.conjunction();

        if (fileName != null && !fileName.isBlank()) {
            String like = "%" + fileName.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("fileName")), like));
        }
        if (fileType != null && !fileType.isBlank()) {
            String like = "%" + fileType.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("fileType")), like));
        }

        return attachmentRepository.findAll(spec, pageable).map(attachmentMapper::toCreatedAtResponse);
    }

    private void assertSortAllowed(Pageable pageable, Set<String> allowed) {
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Sorting by '" + order.getProperty() + "' is not allowed");
            }
        });
    }

    @Transactional(readOnly = true)
    public AttachmentResponse getById(Long id) {
        return attachmentMapper.toCreatedAtResponse(getEntity(id));
    }

    @Transactional
    public AttachmentResponse update(Long id, AttachmentRequest request) {
        Attachment attachment = getEntity(id);

        attachment.setFileName(request.getFileName());
        attachment.setFileType(request.getFileType());
        attachment.setFilePath(request.getFilePath());
        attachment.setFileSize(request.getFileSize());

        return attachmentMapper.toUpdatedAtResponse(attachmentRepository.save(attachment));
    }

    @Transactional
    public AttachmentResponse upload(MultipartFile file, Long userId, Long productId) throws IOException {
        validateOwnership(userId, productId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }

        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed (received: " + file.getContentType() + ")");
        }

        User user = userId != null ? getUser(userId) : null;
        Product product = productId != null ? getProduct(productId) : null;

        String filePath = cloudinaryStorageService.upload(file);

        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment = attachmentRepository.save(attachment);

        if (user != null) {
            UserAttachment userAttachment = new UserAttachment();
            userAttachment.setUser(user);
            userAttachment.setAttachment(attachment);
            userAttachmentRepository.save(userAttachment);
        }

        if (product != null) {
            ProductAttachment productAttachment = new ProductAttachment();
            productAttachment.setProduct(product);
            productAttachment.setAttachment(attachment);
            productAttachmentRepository.save(productAttachment);
        }

        return attachmentMapper.toCreatedAtResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getByUserId(Long userId) {
        return attachmentRepository.findByUserAttachmentsUserId(userId).stream()
                .map(attachmentMapper::toCreatedAtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getByProductId(Long productId) {
        return attachmentRepository.findByProductAttachmentsProductId(productId).stream()
                .map(attachmentMapper::toCreatedAtResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Attachment attachment = getEntity(id);

        userAttachmentRepository.deleteByAttachmentId(id);
        productAttachmentRepository.deleteByAttachmentId(id);

        cloudinaryStorageService.delete(attachment.getFilePath());

        attachmentRepository.delete(attachment);
    }

    private void validateOwnership(Long userId, Long productId) {
        boolean hasUser = userId != null;
        boolean hasProduct = productId != null;

        if (hasUser == hasProduct) {
            throw new IllegalArgumentException(
                    "Exactly one owner must be provided: provide either userId or productId (not both, not neither)");
        }
    }

    private Attachment getEntity(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }
}
