package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.UserRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.model.User;

@Component
public class UserMapper {

    public User toEntity(UserRequest dto){
        User user = new User();

        user.setName(dto.getUserName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setPassword_hash(dto.getPassword_hash());

        return user;
    }

    public UserResponse toResponse(User user){
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setCreated_at(user.getCreatedAt());
        response.setUpdated_at(user.getUpdatedAt());

        return response;
    }

    public UserResponse toSummaryResponse(User user){
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);

        return response;
    }

    public UserResponse toCreatedAtResponse(User user){
        UserResponse response = toResponse(user);
        response.setUpdated_at(null);
        return response;
    }

    public UserResponse toUpdatedAtResponse(User user){
        UserResponse response = toResponse(user);
        response.setCreated_at(null);
        return response;
    }
}
