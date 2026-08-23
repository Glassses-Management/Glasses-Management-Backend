package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.InventoryRequest;
import com.example.spring_boot_project_api.dto.response.InventoryResponse;
import com.example.spring_boot_project_api.model.Inventory;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setProduct_id(request.getProduct_id());
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
        response.setProduct_id(inventory.getProduct_id());
        response.setQuantity(inventory.getQuantity());
        response.setReorder_threshold(inventory.getReorder_threshold());
        response.setBranch_id(inventory.getBranch() != null ? inventory.getBranch().getId() : null);
        response.setLast_restocked_at(inventory.getLast_restocked_at());
        return response;
    }
}
