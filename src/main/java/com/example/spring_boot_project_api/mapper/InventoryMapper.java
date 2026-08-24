package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.InventoryRequest;
import com.example.spring_boot_project_api.dto.response.InventoryResponse;
import com.example.spring_boot_project_api.model.Inventory;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setQuantity(request.getQuantity());
        inventory.setReorder_threshold(request.getReorder_threshold());
        return inventory;
    }

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setProduct_id(inventory.getProduct() != null ? inventory.getProduct().getId() : null);
        response.setQuantity(inventory.getQuantity());
        response.setReorder_threshold(inventory.getReorder_threshold());
        response.setBranch_id(inventory.getBranch() != null ? inventory.getBranch().getId() : null);
        response.setLast_restocked_at(inventory.getLast_restocked_at());
        response.setCreated_at(inventory.getCreatedAt());
        response.setUpdated_at(inventory.getUpdatedAt());
        return response;
    }

    public InventoryResponse toCreatedAtResponse(Inventory inventory) {
        InventoryResponse response = toResponse(inventory);
        if (response == null) {
            return null;
        }
        response.setUpdated_at(null);
        return response;
    }

    public InventoryResponse toUpdatedAtResponse(Inventory inventory) {
        InventoryResponse response = toResponse(inventory);
        if (response == null) {
            return null;
        }
        response.setCreated_at(null);
        return response;
    }
}
