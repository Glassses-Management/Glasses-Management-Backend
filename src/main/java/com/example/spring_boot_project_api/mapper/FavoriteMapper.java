package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.FavoriteResponse;
import com.example.spring_boot_project_api.model.Favorite;

@Component
public class FavoriteMapper {

    private final ProductMapper productMapper;

    public FavoriteMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public FavoriteResponse toResponse(Favorite favorite) {
        if (favorite == null) {
            return null;
        }

        FavoriteResponse response = new FavoriteResponse();

        response.setId(favorite.getId());
        // Read through the association rather than the id column, which is not
        // mapped on the entity: Hibernate exposes the owning side only.
        response.setProductId(favorite.getProduct() != null ? favorite.getProduct().getId() : null);
        response.setProduct(productMapper.toResponse(favorite.getProduct()));
        response.setCreatedAt(favorite.getCreatedAt());

        return response;
    }
}
