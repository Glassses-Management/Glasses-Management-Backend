package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.UserAttachmentResponse;
import com.example.spring_boot_project_api.service.UserAttachmentService;

@RestController
@RequestMapping("/api")
public class UserAttachmentController {

    private final UserAttachmentService userAttachmentService;

    public UserAttachmentController(UserAttachmentService userAttachmentService) {
        this.userAttachmentService = userAttachmentService;
    }

    @PostMapping("/users/{userId}/attachments/{attachmentId}")
    public ResponseEntity<UserAttachmentResponse> create(@PathVariable Long userId, @PathVariable Long attachmentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAttachmentService.create(userId, attachmentId));
    }

    @GetMapping("/users/{userId}/attachments")
    public List<UserAttachmentResponse> getByUserId(@PathVariable Long userId) {
        return userAttachmentService.getByUserId(userId);
    }

    @GetMapping("/user-attachments/{id}")
    public UserAttachmentResponse getById(@PathVariable Long id) {
        return userAttachmentService.getById(id);
    }

    @DeleteMapping("/user-attachments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAttachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
