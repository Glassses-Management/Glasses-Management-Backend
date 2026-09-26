package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ChangeMyPasswordRequest;
import com.example.spring_boot_project_api.dto.request.GoogleLoginRequest;
import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.service.AuthService;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication);
    }

    @PostMapping("/google")
    public ResponseEntity<String> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    /**
     * Lets the registration page discover that an address is already registered
     * before it commits to creating an account. Answers without creating a user
     * and without returning a session token.
     */
    @PostMapping("/google/exists")
    public ResponseEntity<Map<String, Boolean>> googleExists(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(Map.of("exists", authService.googleAccountExists(request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangeMyPasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authentication, request));
    }
}
