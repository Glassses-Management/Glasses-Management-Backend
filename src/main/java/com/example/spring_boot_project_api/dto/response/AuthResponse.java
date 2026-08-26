package com.example.spring_boot_project_api.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String access_token;
    private String token_type;
    private Instant expires_at;
    private UserResponse user;
}
