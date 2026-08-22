package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private Long branch_id;

    private String name;
    private String email;
    private String role;

    private LocalDateTime created_at;
}
