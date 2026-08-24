package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchResponse {
    private Long id;
    // private Long manager_id;

    private String name;
    private String address;
    private String phone;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
