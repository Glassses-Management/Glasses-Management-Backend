package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BranchRequest {
    
    @NotBlank(message = "Branch name is require")
    @Size(max = 50, message = "Branch name must be under 50 character")
    private String branch_name;

    @NotBlank(message = "Address is require")
    @Size(max = 35, message = "Address must be exsisted under 35 character")
    private String address;

    @NotBlank(message = "Phone number is require")
    @Size(max = 10, message = "Phone muner must be 10")
    private String phone;

    @NotNull(message = "Id is required")
    private Long manager_id;
}
