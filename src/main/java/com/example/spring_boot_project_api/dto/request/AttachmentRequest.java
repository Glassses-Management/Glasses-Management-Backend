package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttachmentRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must be under 255 character")
    private String fileName;

    @Size(max = 100, message = "File type must be under 100 character")
    private String fileType;

    @Size(max = 500, message = "File path must be under 500 character")
    private String filePath;

    private Long fileSize;

}
