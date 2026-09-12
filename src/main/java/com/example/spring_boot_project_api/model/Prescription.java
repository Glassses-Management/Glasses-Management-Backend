package com.example.spring_boot_project_api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "prescription")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Right eye (OD)
    @Column(name = "od_sphere")
    private Float od_sphere;

    @Column(name = "od_cylinder")
    private Float od_cylinder;

    @Column(name = "od_axis")
    private Integer od_axis;

    // Left eye (OS)
    @Column(name = "os_sphere")
    private Float os_sphere;

    @Column(name = "os_cylinder")
    private Float os_cylinder;

    @Column(name = "os_axis")
    private Integer os_axis;

    @Column(name = "near_addition")
    private Float near_addition;

    @Column(name = "pupillary_distance")
    private Float pupillary_distance;

    @Column(length = 255)
    private String notes;

    @Column(name = "prescription_date")
    private LocalDate prescription_date;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
