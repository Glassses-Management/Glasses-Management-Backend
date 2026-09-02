package com.example.spring_boot_project_api.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.AttachmentRequest;
import com.example.spring_boot_project_api.dto.response.AttachmentResponse;
import com.example.spring_boot_project_api.mapper.AttachmentMapper;
import com.example.spring_boot_project_api.model.Attachment;
import com.example.spring_boot_project_api.repository.AttachmentRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final CloudinaryStorageService cloudinaryStorageService;

    public AttachmentService(AttachmentRepository attachmentRepository, AttachmentMapper attachmentMapper,
            CloudinaryStorageService cloudinaryStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentMapper = attachmentMapper;
        this.cloudinaryStorageService = cloudinaryStorageService;
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
    public AttachmentResponse upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }

        String filePath = cloudinaryStorageService.upload(file);

        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());

        return attachmentMapper.toCreatedAtResponse(attachmentRepository.save(attachment));
    }

    @Transactional
    public void delete(Long id) {
        attachmentRepository.delete(getEntity(id));
    }

    private Attachment getEntity(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
    }
}
