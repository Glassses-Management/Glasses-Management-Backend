package com.example.spring_boot_project_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.InventoryRequest;
import com.example.spring_boot_project_api.dto.response.InventoryResponse;
import com.example.spring_boot_project_api.mapper.InventoryMapper;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryRepository inventoryRepository,
            ProductRepository productRepository, InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        Product product = getProduct(request.getProduct_id());
        ensureProductNotDuplicated(request, null);

        Inventory inventory = inventoryMapper.toEntity(request);
        inventory.setProduct(product);
        inventory.setLast_restocked_at(LocalDateTime.now());

        return inventoryMapper.toCreatedAtResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(inventoryMapper::toCreatedAtResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getById(Long id) {
        return inventoryMapper.toCreatedAtResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getByProductId(Long productId) {
        return inventoryRepository.findAllByProductId(productId).stream()
                .map(inventoryMapper::toCreatedAtResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock() {
        return inventoryRepository.findAll().stream()
                .filter(item -> item.getQuantity() <= item.getReorder_threshold())
                .map(inventoryMapper::toCreatedAtResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse update(Long id, InventoryRequest request) {
        Inventory inventory = getEntity(id);
        Product product = getProduct(request.getProduct_id());
        ensureProductNotDuplicated(request, inventory);

        boolean restocked = request.getQuantity() > inventory.getQuantity();

        inventory.setProduct(product);
        inventory.setQuantity(request.getQuantity());
        inventory.setReorder_threshold(request.getReorder_threshold());

        if (restocked) {
            inventory.setLast_restocked_at(LocalDateTime.now());
        }

        return inventoryMapper.toUpdatedAtResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public void delete(Long id) {
        inventoryRepository.delete(getEntity(id));
    }

    private Inventory getEntity(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    private void ensureProductNotDuplicated(InventoryRequest request, Inventory current) {
        inventoryRepository.findByProductId(request.getProduct_id())
                .ifPresent(existing -> {
                    if (current == null || !existing.getId().equals(current.getId())) {
                        throw new IllegalArgumentException("This product already exists in the inventory");
                    }
                });
    }
}
