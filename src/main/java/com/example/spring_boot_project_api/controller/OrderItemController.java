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

import com.example.spring_boot_project_api.dto.request.OrderItemRequest;
import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.dto.response.OrderItemResponse;
import com.example.spring_boot_project_api.dto.response.ProductResponse;
import com.example.spring_boot_project_api.service.OrderItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @PostMapping
    public ResponseEntity<OrderItemResponse> create(@Valid @RequestBody OrderItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<OrderItemResponse>> createBulk(@Valid @RequestBody List<@Valid OrderItemRequest> request){
        List<OrderItemResponse> saved = request.stream().map(orderItemService::create).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<OrderItemResponse> getAll(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long productId) {
        if (orderId != null) {
            return orderItemService.getByOrderId(orderId);
        }
        if (productId != null) {
            return orderItemService.getByProductId(productId);
        }
        return orderItemService.getAll();
    }

    @GetMapping("/{id}")
    public OrderItemResponse getById(@PathVariable Long id) {
        return orderItemService.getById(id);
    }

    @PutMapping("/{id}")
    public OrderItemResponse update(@PathVariable Long id, @Valid @RequestBody OrderItemRequest request) {
        return orderItemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
