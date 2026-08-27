package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AppointmentRequest;
import com.example.spring_boot_project_api.dto.response.AppointmentResponse;
import com.example.spring_boot_project_api.mapper.AppointmentMapper;
import com.example.spring_boot_project_api.model.Appointment;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              CustomerRepository customerRepository,
                              UserRepository userRepository,
                              AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        User optometrist = null;
        if (request.getOptometrist_id() != null) {
            optometrist = userRepository.findById(request.getOptometrist_id())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setCustomer(customer);
        appointment.setOptometrist(optometrist);

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAll().stream().map(appointmentMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        return appointmentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = getEntity(id);

        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        User optometrist = null;
        if (request.getOptometrist_id() != null) {
            optometrist = userRepository.findById(request.getOptometrist_id())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
        }

        appointment.setCustomer(customer);
        appointment.setOptometrist(optometrist);
        appointment.setScheduledAt(request.getScheduled_at());
        appointment.setStatus(request.getStatus());
        appointment.setNotes(request.getNotes());

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    public void delete(Long id) {
        appointmentRepository.delete(getEntity(id));
    }

    private Appointment getEntity(Long id) {
        return appointmentRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));
    }
}
