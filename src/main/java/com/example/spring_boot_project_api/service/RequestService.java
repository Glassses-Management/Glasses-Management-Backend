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
import com.example.spring_boot_project_api.model.Request;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.RequestRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;

@Service
public class RequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String TYPE_EXAM = "exam";
    private static final String TYPE_PRODUCT = "product";

    private static final String APPOINTMENT_STATUS_PENDING_SCHEDULING = "PENDING_SCHEDULING";

    private final RequestRepository requestRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final RequestMapper requestMapper;

    public RequestService(RequestRepository requestRepository,
                          CustomerRepository customerRepository,
                          AppointmentRepository appointmentRepository,
                          RequestMapper requestMapper) {
        this.requestRepository = requestRepository;
        this.customerRepository = customerRepository;
        this.appointmentRepository = appointmentRepository;
        this.requestMapper = requestMapper;
    }

    @Transactional
    public RequestResponse submit(Long userId, String role, RequestRequest request) {
        Customer customer;
        if (request.getCustomerId() != null) {
            if (!("ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role))) {
                throw new AccessDeniedException(
                        "Only staff or admin can submit a request on behalf of a customer");
            }
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Customer not found for id: " + request.getCustomerId()));
        } else {
            customer = resolveCustomerByUserId(userId);
        }

        String type = request.getType() == null ? null : request.getType().trim().toLowerCase();
        if (type == null || !(TYPE_EXAM.equals(type) || TYPE_PRODUCT.equals(type))) {
            throw new IllegalArgumentException("Request type must be 'exam' or 'product'");
        }

        Request entity = requestMapper.toEntity(request);
        entity.setCustomer(customer);
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

        if (TYPE_EXAM.equalsIgnoreCase(request.getType())
                && appointmentRepository.existsByRequestId(requestId)) {
            throw new IllegalStateException("An appointment already exists for this request");
        }

        request.setStatus(STATUS_APPROVED);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);

        if (TYPE_EXAM.equalsIgnoreCase(request.getType())) {
            Appointment appointment = new Appointment();
            appointment.setCustomer(request.getCustomer());
            appointment.setStatus(APPOINTMENT_STATUS_PENDING_SCHEDULING);
            appointment.setNotes(request.getNotes());
            appointment.setRequest(request);
            appointment.setScheduledAt(null);
            appointment.setOptometrist(null);

            appointmentRepository.save(appointment);
        }

        return requestMapper.toResponse(request);
    }

    @Transactional
    public RequestResponse reject(Long requestId) {
        Request request = getEntity(requestId);

        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }

        request.setStatus(STATUS_REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);
        return requestMapper.toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getAll() {
        return requestRepository.findAll().stream().map(requestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getMyRequests(Long userId) {
        Long customerId = resolveCustomerIdByUserId(userId);
        return requestRepository.findByCustomerId(customerId).stream()
                .map(requestMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(Long id, Long userId, String role) {
        Request request = getEntity(id);
        ensureCanRead(request, userId, role);
        return requestMapper.toResponse(request);
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

    private Customer resolveCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));
    }

    private Long resolveCustomerIdByUserId(Long userId) {
        return resolveCustomerByUserId(userId).getId();
    }

    // A customer may only read their own requests. Staff and admins (the request
    // managers) may read any request. This cannot be bypassed by sending a
    // customerId from the frontend because the owner is resolved from the JWT.
    private void ensureCanRead(Request request, Long userId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role)) {
            return;
        }
        Long myCustomerId = resolveCustomerIdByUserId(userId);
        Long ownerCustomerId = request.getCustomer() != null ? request.getCustomer().getId() : null;
        if (!java.util.Objects.equals(myCustomerId, ownerCustomerId)) {
            throw new AccessDeniedException("You can only view your own requests");
        }
    }

    private Request getEntity(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));
    }
}
