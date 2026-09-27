package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    Optional<Favorite> findByCustomerIdAndProductId(Long customerId, Long productId);

    /**
     * Scoped to the customer so removing a favourite can never delete another
     * customer's row. Returns how many rows went, so the service can answer 204
     * whether or not anything matched.
     */
    long deleteByCustomerIdAndProductId(Long customerId, Long productId);
}
