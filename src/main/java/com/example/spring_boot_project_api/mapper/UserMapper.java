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
        user.setPassword_hash(dto.getPassword_hash());
        user.setRole(dto.getRole());

        return user;
    }

    public UserResponse toResponse(User user){
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBranch_id(user.getBranch() != null ? user.getBranch().getId() : null);
        response.setCreated_at(user.getCreated_at());

        return response;
    }
}
