package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.UserAttachmentResponse;
import com.example.spring_boot_project_api.mapper.UserAttachmentMapper;
import com.example.spring_boot_project_api.model.Attachment;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserAttachment;
import com.example.spring_boot_project_api.repository.AttachmentRepository;
import com.example.spring_boot_project_api.repository.UserAttachmentRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UserAttachmentService {

    private final UserAttachmentRepository userAttachmentRepository;
    private final UserAttachmentMapper userAttachmentMapper;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;

    public UserAttachmentService(UserAttachmentRepository userAttachmentRepository,
                                 UserAttachmentMapper userAttachmentMapper,
                                 UserRepository userRepository,
                                 AttachmentRepository attachmentRepository) {
        this.userAttachmentRepository = userAttachmentRepository;
        this.userAttachmentMapper = userAttachmentMapper;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public UserAttachmentResponse create(Long userId, Long attachmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));

        UserAttachment userAttachment = new UserAttachment();
        userAttachment.setUser(user);
        userAttachment.setAttachment(attachment);

        return userAttachmentMapper.toResponse(userAttachmentRepository.save(userAttachment));
    }

    @Transactional(readOnly = true)
    public List<UserAttachmentResponse> getByUserId(Long userId) {
        return userAttachmentRepository.findAll().stream()
                .filter(ua -> ua.getUser() != null && ua.getUser().getId().equals(userId))
                .map(userAttachmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAttachmentResponse getById(Long id) {
        return userAttachmentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public void delete(Long id) {
        userAttachmentRepository.delete(getEntity(id));
    }

    private UserAttachment getEntity(Long id) {
        return userAttachmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UserAttachment not found"));
    }
}
