package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.BranchRequest;
import com.example.spring_boot_project_api.dto.response.BranchResponse;
import com.example.spring_boot_project_api.mapper.BranchMapper;
import com.example.spring_boot_project_api.model.Branch;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.BranchRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class BranchService {
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final BranchMapper branchMapper;

    public BranchService(BranchRepository branchRepository, UserRepository userRepository ,BranchMapper branchMapper){
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.branchMapper = branchMapper;
    }

    @Transactional
    public BranchResponse create(BranchRequest request){
        User manager = userRepository.findById(request.getManager_id()).
                       orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        Branch branch = branchMapper.toEntity(request);
        branch.setManager(manager);

        return branchMapper.toResponse(branchRepository.save(branch));
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAll(){
        return branchRepository.findAll().stream().map(branchMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(Long id){
        return branchMapper.toResponse(getEntity(id));
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request){
        Branch branch = getEntity(id);

        User manager = userRepository.findById(request.getManager_id())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

        branch.setName(request.getBranch_name());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setManager(manager);

        return branchMapper.toResponse(branchRepository.save(branch));
    }

    public void delete(Long id){
        branchRepository.delete(getEntity(id));
    }

    private Branch getEntity(Long id){
        return branchRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
    }

}
