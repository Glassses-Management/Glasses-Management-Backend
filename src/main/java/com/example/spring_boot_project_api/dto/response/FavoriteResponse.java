package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * A saved product as the storefront needs to render it.
 *
 * <p>The product itself is the existing ProductResponse rather than a second copy
 * of the product fields, so a card or detail page can reuse the component it
 * already has without remapping anything.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteResponse {

    private Long id;

    /** Convenience for the client, which tracks favourites by product id. */
    private Long productId;

    private ProductResponse product;

    private LocalDateTime createdAt;
}
