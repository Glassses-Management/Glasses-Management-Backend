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
        response.setCreated_at(branch.getCreatedAt());
        response.setUpdated_at(branch.getUpdatedAt());
        // response.setManager_id(branch.getManager() != null ? branch.getManager().getId() : null);

        return response;
    }

    public BranchResponse toCreatedAtResponse(Branch branch) {
        BranchResponse response = toResponse(branch);
        response.setUpdated_at(null);
        return response;
    }

    public BranchResponse toUpdatedAtResponse(Branch branch) {
        BranchResponse response = toResponse(branch);
        response.setCreated_at(null);
        return response;
    }
}
