package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.UserRequest;
import com.example.spring_boot_project_api.dto.request.UserUpdateRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityExistsException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.Role;

@Service
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder){
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(UserRequest request){
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        User user = userMapper.toEntity(request);
        user.setPassword_hash(passwordEncoder.encode(request.getPassword_hash()));
        user.setRole(parseRole(request.getRole()));

        return userMapper.toCreatedAtResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll(){
        return userRepository.findAll().stream().map(userMapper::toCreatedAtResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id){
        return userMapper.toCreatedAtResponse(getEntity(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request){
        User user = getEntity(id);

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())
                && !request.getPhone().equals(user.getPhone())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        user.setName(request.getUserName());
        user.setEmail(request.getEmail());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }
        user.setRole(parseRole(request.getRole()));

        return userMapper.toUpdatedAtResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse changePassword(Long id, String newPassword){
        User user = getEntity(id);
        user.setPassword_hash(passwordEncoder.encode(newPassword));
        return userMapper.toUpdatedAtResponse(userRepository.save(user));
    }

    public void delete(Long id){
        userRepository.delete(getEntity(id));
    }

    private User getEntity(Long id){
        return userRepository.findById(id).
                orElseThrow(() -> new EntityExistsException("User not found"));
    }

    private Role parseRole(String role){
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid role. Allowed values: ADMIN, STAFF, OPTOMETRIST");
        }
    }
}
