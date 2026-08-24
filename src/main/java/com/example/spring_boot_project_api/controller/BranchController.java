package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.BranchRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRequest;
import com.example.spring_boot_project_api.dto.response.BranchResponse;
import com.example.spring_boot_project_api.dto.response.CustomerResponse;
import com.example.spring_boot_project_api.service.BranchService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
    }
    
    @PostMapping("/bulk")
    public ResponseEntity<List<BranchResponse>> createBulk(@Valid @RequestBody List<@Valid BranchRequest> request){
        List<BranchResponse> saved = request.stream().map(branchService::create).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<BranchResponse> getAll() {
        return branchService.getAll();
    }

    @GetMapping("/{id}")
    public BranchResponse getById(@PathVariable Long id) {
        return branchService.getById(id);
    }

    @PutMapping("/{id}")
    public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return branchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
