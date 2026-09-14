package com.example.spring_boot_project_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RequestRequest;
import com.example.spring_boot_project_api.dto.response.RequestResponse;
import com.example.spring_boot_project_api.mapper.RequestMapper;
import com.example.spring_boot_project_api.model.Appointment;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Prescription;
import com.example.spring_boot_project_api.model.Request;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.PrescriptionRepository;
import com.example.spring_boot_project_api.repository.RequestRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final RequestRepository requestRepository;
    private final CustomerRepository customerRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final RequestMapper requestMapper;

    public RequestService(RequestRepository requestRepository,
                          CustomerRepository customerRepository,
                          PrescriptionRepository prescriptionRepository,
                          AppointmentRepository appointmentRepository,
                          RequestMapper requestMapper) {
        this.requestRepository = requestRepository;
        this.customerRepository = customerRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.requestMapper = requestMapper;
    }

    @Transactional
    public RequestResponse submit(Long userId, RequestRequest request) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));

        String type = request.getType() == null ? null : request.getType().trim().toLowerCase();
        if (type == null || !("exam".equals(type) || "product".equals(type))) {
            throw new IllegalArgumentException("Request type must be 'exam' or 'product'");
        }

        Prescription prescription = null;
        if (request.getPrescriptionId() != null) {
            prescription = prescriptionRepository.findById(request.getPrescriptionId())
                    .filter(p -> p.getCustomer() != null
                            && p.getCustomer().getId().equals(customer.getId()))
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Prescription not found for id: " + request.getPrescriptionId()));
        }

        Request entity = requestMapper.toEntity(request);
        entity.setCustomer(customer);
        entity.setPrescription(prescription);
        entity.setStatus(STATUS_PENDING);

        Request saved = requestRepository.save(entity);
        return requestMapper.toResponse(saved);
    }

    @Transactional
    public RequestResponse approve(Long requestId) {
        Request request = getEntity(requestId);

        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }

        if (appointmentRepository.existsByRequestId(requestId)) {
            throw new IllegalStateException("An appointment already exists for this request");
        }

        request.setStatus(STATUS_APPROVED);
        Request approved = requestRepository.save(request);

        Appointment appointment = new Appointment();
        appointment.setCustomer(approved.getCustomer());
        appointment.setStatus("PENDING_REVIEW");
        appointment.setNotes(approved.getNotes());
        appointment.setRequest(approved);
        appointment.setScheduledAt(null);
        appointment.setOptometrist(null);
        appointment.setCreatedAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        return requestMapper.toResponse(approved);
    }

    @Transactional
    public RequestResponse reject(Long requestId) {
        Request request = getEntity(requestId);

        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }

        request.setStatus(STATUS_REJECTED);
        Request rejected = requestRepository.save(request);
        return requestMapper.toResponse(rejected);
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getAll() {
        return requestRepository.findAll().stream().map(requestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(Long id) {
        return requestMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getByCustomerId(Long customerId) {
        return requestRepository.findByCustomerId(customerId).stream()
                .map(requestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getByStatus(String status) {
        return requestRepository.findByStatus(status).stream()
                .map(requestMapper::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        requestRepository.delete(getEntity(id));
    }

    private Request getEntity(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
    }
}
