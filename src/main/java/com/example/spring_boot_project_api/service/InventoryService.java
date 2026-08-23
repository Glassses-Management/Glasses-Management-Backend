package com.example.spring_boot_project_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.InventoryRequest;
import com.example.spring_boot_project_api.dto.response.InventoryResponse;
import com.example.spring_boot_project_api.mapper.InventoryMapper;
import com.example.spring_boot_project_api.model.Branch;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.repository.BranchRepository;
import com.example.spring_boot_project_api.repository.InventoryRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryRepository inventoryRepository, BranchRepository branchRepository,
            InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        ensureProductNotDuplicated(request, null);
        Branch branch = getBranch(request.getBranch_id());

        Inventory inventory = inventoryMapper.toEntity(request);
        inventory.setBranch(branch);
        inventory.setLast_restocked_at(LocalDateTime.now());

        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(inventoryMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getById(Long id) {
        return inventoryMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getByBranch(Long branchId) {
        getBranch(branchId);
        return inventoryRepository.findAllByBranch_Id(branchId).stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock() {
        return inventoryRepository.findAll().stream()
                .filter(item -> item.getQuantity() <= item.getReorder_threshold())
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse update(Long id, InventoryRequest request) {
        Inventory inventory = getEntity(id);
        ensureProductNotDuplicated(request, inventory);
        Branch branch = getBranch(request.getBranch_id());

        boolean restocked = request.getQuantity() > inventory.getQuantity();

        inventory.setProduct_id(request.getProduct_id());
        inventory.setQuantity(request.getQuantity());
        inventory.setReorder_threshold(request.getReorder_threshold());
        inventory.setBranch(branch);

        if (restocked) {
            inventory.setLast_restocked_at(LocalDateTime.now());
        }

        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public void delete(Long id) {
        inventoryRepository.delete(getEntity(id));
    }

    private Inventory getEntity(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
    }

    private Branch getBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
    }

    private void ensureProductNotDuplicated(InventoryRequest request, Inventory current) {
        Long branchId = current != null && request.getBranch_id() == null
                ? current.getBranch().getId()
                : request.getBranch_id();
        if (branchId == null) {
            return;
        }
        inventoryRepository.findByProduct_idAndBranch_Id(request.getProduct_id(), branchId)
                .ifPresent(existing -> {
                    if (current == null || !existing.getId().equals(current.getId())) {
                        throw new IllegalArgumentException("This product already exists in the branch inventory");
                    }
                });
    }
}
