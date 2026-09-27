package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.FavoriteResponse;
import com.example.spring_boot_project_api.mapper.FavoriteMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Favorite;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.FavoriteRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final FavoriteMapper favoriteMapper;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           FavoriteMapper favoriteMapper) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.favoriteMapper = favoriteMapper;
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(Authentication authentication) {
        Long customerId = currentCustomerId(authentication);
        return favoriteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(favoriteMapper::toResponse)
                .toList();
    }

    /**
     * Saves a product, or returns the existing row when it is already saved.
     *
     * <p>Idempotent on purpose: the heart is a toggle that a customer may double
     * click, and a retried request should not fail or duplicate. The unique
     * constraint backs this up when two requests race.
     */
    @Transactional
    public FavoriteResponse add(Authentication authentication, Long productId) {
        Customer customer = currentCustomer(authentication);

        Optional<Favorite> existing = favoriteRepository
                .findByCustomerIdAndProductId(customer.getId(), productId);
        if (existing.isPresent()) {
            return favoriteMapper.toResponse(existing.get());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Favorite favorite = new Favorite();
        favorite.setCustomer(customer);
        favorite.setProduct(product);

        return favoriteMapper.toResponse(favoriteRepository.save(favorite));
    }

    /**
     * Removes a saved product. Removing something that is not saved is not an
     * error: the caller's intent is satisfied either way, so this stays quiet
     * rather than reporting a 404 the UI cannot act on.
     */
    @Transactional
    public void remove(Authentication authentication, Long productId) {
        Long customerId = currentCustomerId(authentication);
        favoriteRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Authentication authentication, Long productId) {
        Long customerId = currentCustomerId(authentication);
        return favoriteRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    /**
     * Resolves the customer from the verified token and never from the request.
     *
     * <p>The JWT subject is the account email and the token also carries the user
     * id as {@code uid}. The id is preferred because it is what links to the
     * customer row; the email is only a fallback. Nothing supplied by the caller
     * takes part, so a customer cannot address another customer's favourites by
     * editing a request.
     */
    private Customer currentCustomer(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                // An anonymous token reports itself as authenticated, so it has
                // to be rejected by type. The security rules already keep it out
                // of these endpoints; this is the second line.
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Not authenticated");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String uid = jwtAuthentication.getToken().getClaimAsString("uid");
            if (uid != null && !uid.isBlank()) {
                return customerRepository.findByUserId(Long.valueOf(uid))
                        .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));
            }
        }

        return customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));
    }

    private Long currentCustomerId(Authentication authentication) {
        return currentCustomer(authentication).getId();
    }
}
