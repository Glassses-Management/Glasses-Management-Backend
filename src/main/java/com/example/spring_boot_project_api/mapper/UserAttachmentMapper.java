package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.UserAttachmentResponse;
import com.example.spring_boot_project_api.model.UserAttachment;

@Component
public class UserAttachmentMapper {

    public UserAttachmentResponse toResponse(UserAttachment userAttachment) {
        if (userAttachment == null) {
            return null;
        }
        UserAttachmentResponse response = new UserAttachmentResponse();
        response.setId(userAttachment.getId());
        response.setUserId(userAttachment.getUser() != null ? userAttachment.getUser().getId() : null);
        response.setAttachmentId(userAttachment.getAttachment() != null ? userAttachment.getAttachment().getId() : null);
        response.setCreated_at(userAttachment.getCreatedAt());
        return response;
    }
}
