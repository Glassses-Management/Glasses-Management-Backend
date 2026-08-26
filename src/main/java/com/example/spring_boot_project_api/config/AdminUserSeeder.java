package com.example.spring_boot_project_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.Branch;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.BranchRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.branch-id:0}")
    private Long adminBranchId;

    public AdminUserSeeder(UserRepository userRepository,
                           BranchRepository branchRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL or ADMIN_PASSWORD not set, skipping admin seeding");
            return;
        }

        if (userRepository.count() > 0) {
            return;
        }

        Branch branch = branchRepository.findById(adminBranchId).orElse(null);
        if (branch == null) {
            log.warn("ADMIN_BRANCH_ID {} does not exist, skipping admin seeding", adminBranchId);
            return;
        }

        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(adminEmail);
        admin.setPassword_hash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setBranch(branch);

        userRepository.save(admin);
        log.info("Seeded initial admin user {}", adminEmail);
    }
}
