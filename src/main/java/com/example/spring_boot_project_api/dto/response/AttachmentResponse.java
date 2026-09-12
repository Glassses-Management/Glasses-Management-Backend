package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentResponse {
    private Long id;

    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
