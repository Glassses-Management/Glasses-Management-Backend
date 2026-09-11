package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CustomerRequestRequest;
import com.example.spring_boot_project_api.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final OrderService orderService;

    public RequestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Object> submit(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CustomerRequestRequest request) {
        Object uidClaim = jwt.getClaim("uid");
        if (!(uidClaim instanceof Number)) {
            throw new IllegalArgumentException("Invalid token: missing user id");
        }
        Long uid = ((Number) uidClaim).longValue();
        Object result = orderService.submitCustomerRequest(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}