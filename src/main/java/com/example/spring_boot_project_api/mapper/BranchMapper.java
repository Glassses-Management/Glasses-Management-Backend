package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.BranchRequest;
import com.example.spring_boot_project_api.dto.response.BranchResponse;
import com.example.spring_boot_project_api.model.Branch;

@Component
public class BranchMapper {

    public Branch toEntity(BranchRequest dto) {
        Branch branch = new Branch();

        branch.setName(dto.getBranch_name());
        branch.setAddress(dto.getAddress());
        branch.setPhone(dto.getPhone());

        return branch;
    }

    public BranchResponse toResponse(Branch branch) {
        BranchResponse response = new BranchResponse();

        response.setId(branch.getId());
        response.setName(branch.getName());
        response.setAddress(branch.getAddress());
        response.setPhone(branch.getPhone());
        response.setManager_id(branch.getManager() != null ? branch.getManager().getId() : null);

        return response;
    }
}
