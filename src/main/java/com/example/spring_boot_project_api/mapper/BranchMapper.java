package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.BranchRequest;
import com.example.spring_boot_project_api.dto.response.BranchRespone;
import com.example.spring_boot_project_api.model.Branch;


@Component
public class BranchMapper {
    public Branch toEntity(BranchRequest dto){
        Branch branch = new Branch();

        branch.setName(dto.getBranch_name());
        return branch;
    }
    public Branch toRespone(BranchRespone dto){
        Branch branch = new Branch();

        branch.setId(dto.getId());
        branch.setName(dto.getName());

        return branch;
    }
}
