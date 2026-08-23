package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping
    public List<OrderResponse> getAll(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status) {
        if (customerId != null) {
            return orderService.getByCustomerId(customerId);
        }
        if (branchId != null) {
            return orderService.getByBranchId(branchId);
        }
        if (status != null && !status.isBlank()) {
            return orderService.getByStatus(status);
        }
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
