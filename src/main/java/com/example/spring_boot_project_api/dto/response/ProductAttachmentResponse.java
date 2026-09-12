package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAttachmentResponse {
    private Long id;
    private Long productId;
    private Long attachmentId;
    private LocalDateTime created_at;
}
