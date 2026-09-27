package com.example.spring_boot_project_api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.FavoriteResponse;
import com.example.spring_boot_project_api.service.FavoriteService;

/**
 * Wishlist endpoints for the signed-in customer.
 *
 * <p>No endpoint here accepts a customer id. The owner is taken from the
 * authenticated principal on every call, so there is no identifier a caller could
 * change to reach somebody else's wishlist.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> list(Authentication authentication) {
        return favoriteService.list(authentication);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FavoriteResponse> add(Authentication authentication,
                                                @PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.add(authentication, productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(Authentication authentication,
                                       @PathVariable Long productId) {
        favoriteService.remove(authentication, productId);
        return ResponseEntity.noContent().build();
    }

    /** Single-product check, for callers that are not holding the full list. */
    @GetMapping("/{productId}")
    public Map<String, Boolean> isFavorite(Authentication authentication,
                                           @PathVariable Long productId) {
        return Map.of("favorited", favoriteService.isFavorite(authentication, productId));
    }
}
