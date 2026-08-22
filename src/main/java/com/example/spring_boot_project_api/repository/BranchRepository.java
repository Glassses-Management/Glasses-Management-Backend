package com.example.spring_boot_project_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {

}
