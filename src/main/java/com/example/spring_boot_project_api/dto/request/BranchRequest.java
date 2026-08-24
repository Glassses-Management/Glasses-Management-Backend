package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BranchRequest {
    
    @NotBlank(message = "Branch name is require")
    @Size(max = 50, message = "Branch name must be under 50 character")
    private String branch_name;

    @NotBlank(message = "Address is require")
    @Size(max = 100, message = "Address must be exsisted under 100 character")
    private String address;

    @NotBlank(message = "Phone number is require")
    @Size(max = 20, message = "Phone muner must be under 20")
    private String phone;

    //     private Long manager_id;

}
