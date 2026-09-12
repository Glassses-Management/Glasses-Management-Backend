package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;

    private String name;
    private String email;
    private String role;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
