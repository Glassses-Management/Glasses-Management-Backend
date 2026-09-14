package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RequestRequest;
import com.example.spring_boot_project_api.dto.response.RequestResponse;
import com.example.spring_boot_project_api.service.RequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<RequestResponse> submit(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody RequestRequest request) {
        Object uidClaim = jwt.getClaim("uid");
        if (!(uidClaim instanceof Number)) {
            throw new IllegalArgumentException("Invalid token: missing user id");
        }
        Long uid = ((Number) uidClaim).longValue();
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.submit(uid, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.reject(id));
    }

    @GetMapping
    public List<RequestResponse> getAll() {
        return requestService.getAll();
    }

    @GetMapping("/{id}")
    public RequestResponse getById(@PathVariable Long id) {
        return requestService.getById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<RequestResponse> getByCustomerId(@PathVariable Long customerId) {
        return requestService.getByCustomerId(customerId);
    }

    @GetMapping("/status/{status}")
    public List<RequestResponse> getByStatus(@PathVariable String status) {
        return requestService.getByStatus(status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
