package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Appointment;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByCustomerId(Long customerId);

    List<Appointment> findByStatus(String status);

    boolean existsByOptometristIdAndScheduledAt(Long optometristId, LocalDateTime scheduledAt);

    boolean existsByOptometristIdAndScheduledAtAndIdNot(Long optometristId, LocalDateTime scheduledAt, Long id);

    boolean existsByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime start, LocalDateTime end);
}
