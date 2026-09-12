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
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.InventoryRequest;
import com.example.spring_boot_project_api.dto.response.InventoryResponse;
import com.example.spring_boot_project_api.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<InventoryResponse>> createBulk(@Valid @RequestBody List<@Valid InventoryRequest> request){
        List<InventoryResponse> saved = request.stream().map(inventoryService::create).toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<InventoryResponse> getAll() {
        return inventoryService.getAll();
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> getLowStock() {
        return inventoryService.getLowStock();
    }

    @GetMapping("/{id}")
    public InventoryResponse getById(@PathVariable Long id) {
        return inventoryService.getById(id);
    }

    @PutMapping("/{id}")
    public InventoryResponse update(@PathVariable Long id, @Valid @RequestBody InventoryRequest request) {
        return inventoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
