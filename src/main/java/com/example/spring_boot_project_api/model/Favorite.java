package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

/**
 * A product saved by a customer to their wishlist.
 *
 * <p>Both sides are references rather than copies, so a price or model change is
 * reflected immediately instead of going stale in a duplicated row. The unique
 * constraint mirrors uq_favorite_customer_product in the migration and is what
 * the service relies on to keep favouriting idempotent.
 */
@Entity
@Data
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(
        name = "uq_favorite_customer_product",
        columnNames = {"customer_id", "product_id"}))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
