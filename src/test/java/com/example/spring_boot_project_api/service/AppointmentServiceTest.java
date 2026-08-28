package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.AppointmentRequest;
import com.example.spring_boot_project_api.dto.response.AppointmentResponse;
import com.example.spring_boot_project_api.mapper.AppointmentMapper;
import com.example.spring_boot_project_api.model.Appointment;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    private final LocalDateTime when = LocalDateTime.of(2024, 6, 1, 10, 0);

    private AppointmentRequest sampleRequest(Long optometristId) {
        AppointmentRequest request = new AppointmentRequest();
        request.setCustomer_id(1L);
        request.setOptometrist_id(optometristId);
        request.setScheduled_at(when);
        request.setStatus("PENDING");
        return request;
    }

    @BeforeEach
    void setUp() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(new Customer()));
        when(userRepository.findById(5L)).thenReturn(Optional.of(new User()));
        when(appointmentMapper.toEntity(any(AppointmentRequest.class))).thenAnswer(inv -> new Appointment());
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(new AppointmentResponse());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_succeedsWhenOptometristFree() {
        when(appointmentRepository.existsByOptometristIdAndScheduledAt(5L, when)).thenReturn(false);

        assertThat(appointmentService.create(sampleRequest(5L))).isNotNull();
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void create_throwsWhenOptometristDoubleBooked() {
        when(appointmentRepository.existsByOptometristIdAndScheduledAt(5L, when)).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(sampleRequest(5L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an appointment");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void update_allowsSameTimeForSameAppointment() {
        Appointment existing = new Appointment();
        existing.setId(1L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByOptometristIdAndScheduledAtAndIdNot(5L, when, 1L)).thenReturn(false);

        assertThat(appointmentService.update(1L, sampleRequest(5L))).isNotNull();
    }

    @Test
    void update_throwsWhenConflictingWithAnotherAppointment() {
        Appointment existing = new Appointment();
        existing.setId(1L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsByOptometristIdAndScheduledAtAndIdNot(5L, when, 1L)).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.update(1L, sampleRequest(5L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an appointment");
        verify(appointmentRepository, never()).save(any());
    }
}
