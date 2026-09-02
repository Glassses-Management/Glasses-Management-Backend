package com.example.spring_boot_project_api.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.AttachmentRequest;
import com.example.spring_boot_project_api.dto.response.AttachmentResponse;
import com.example.spring_boot_project_api.service.AttachmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public ResponseEntity<AttachmentResponse> create(@Valid @RequestBody AttachmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AttachmentResponse>> createBulk(@Valid @RequestBody List<@Valid AttachmentRequest> request){
        List<AttachmentResponse> saved = request.stream().map(attachmentService::create).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(file));
    }

    @GetMapping
    public Page<AttachmentResponse> search(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileType,
            Pageable pageable) {
        return attachmentService.search(fileName, fileType, pageable);
    }
    @GetMapping("/{id}")
    public AttachmentResponse getById(@PathVariable Long id) {
        return attachmentService.getById(id);
    }

    @PutMapping("/{id}")
    public AttachmentResponse update(@PathVariable Long id, @Valid @RequestBody AttachmentRequest request) {
        return attachmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
