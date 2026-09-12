package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.AttachmentRequest;
import com.example.spring_boot_project_api.dto.response.AttachmentResponse;
import com.example.spring_boot_project_api.model.Attachment;

@Component
public class AttachmentMapper {

    public Attachment toEntity(AttachmentRequest request) {
        Attachment attachment = new Attachment();
        attachment.setFileName(request.getFileName());
        attachment.setFileType(request.getFileType());
        attachment.setFilePath(request.getFilePath());
        attachment.setFileSize(request.getFileSize());
        return attachment;
    }

    public AttachmentResponse toResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setFileName(attachment.getFileName());
        response.setFileType(attachment.getFileType());
        response.setFilePath(attachment.getFilePath());
        response.setFileSize(attachment.getFileSize());
        response.setCreated_at(attachment.getCreatedAt());
        response.setUpdated_at(attachment.getUpdatedAt());
        return response;
    }

    public AttachmentResponse toCreatedAtResponse(Attachment attachment) {
        AttachmentResponse response = toResponse(attachment);
        if (response == null) {
            return null;
        }
        response.setUpdated_at(null);
        return response;
    }

    public AttachmentResponse toUpdatedAtResponse(Attachment attachment) {
        AttachmentResponse response = toResponse(attachment);
        if (response == null) {
            return null;
        }
        response.setCreated_at(null);
        return response;
    }
}
