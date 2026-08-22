package com.example.spring_boot_project_api.dto.response;

import lombok.Data;

@Data
public class BranchRespone {
    private Long id;

    private String name;

    private String address;

    private String phone;
}
