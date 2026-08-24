package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {
    private Long id;

    private String name;
    private String phone;
    private String email;
    private String address;

    private LocalDate date_of_birth;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
