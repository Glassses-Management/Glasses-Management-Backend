package com.example.spring_boot_project_api.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.security.JwtService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email/phone or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword_hash())) {
            throw new BadCredentialsException("Invalid email/phone or password");
        }

        return jwtService.generateToken(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }
}
