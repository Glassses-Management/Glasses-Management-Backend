package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.RequestRequest;
import com.example.spring_boot_project_api.dto.response.RequestResponse;
import com.example.spring_boot_project_api.mapper.RequestMapper;
import com.example.spring_boot_project_api.model.Appointment;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Request;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.RequestRepository;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private RequestService requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(
                requestRepository, customerRepository, appointmentRepository, new RequestMapper());
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Test Customer");
        return customer;
    }

    private Request pendingRequest(Long id, String type) {
        Request request = new Request();
        request.setId(id);
        request.setType(type);
        request.setStatus("PENDING");
        request.setNotes("notes");
        request.setCustomer(customer(5L));
        return request;
    }

    private RequestRequest examRequest() {
        RequestRequest request = new RequestRequest();
        request.setType("exam");
        request.setNotes("I would like to have my vision checked.");
        return request;
    }

    @Test
    void submit_examWithoutPrescription_createsPendingRequest() {
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(5L)));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response = requestService.submit(1L, "CUSTOMER", examRequest());

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(captor.capture());
        Request saved = captor.getValue();

        assertEquals("exam", saved.getType());
        assertEquals("PENDING", saved.getStatus());
        assertEquals(5L, saved.getCustomer().getId());
        assertNull(saved.getPrescription());

        assertNull(response.getUpdatedAt());

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void submit_adminWithCustomerId_usesGivenCustomer() {
        RequestRequest request = examRequest();
        request.setCustomerId(5L);

        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer(5L)));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        requestService.submit(1L, "ADMIN", request);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getCustomer().getId());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertNull(captor.getValue().getPrescription());
    }

    @Test
    void submit_customerWithCustomerId_isDenied() {
        RequestRequest request = examRequest();
        request.setCustomerId(5L);

        assertThrows(AccessDeniedException.class, () -> requestService.submit(1L, "CUSTOMER", request));
    }

    @Test
    void submit_invalidType_throws() {
        RequestRequest request = examRequest();
        request.setType("glasses");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer(5L)));

        assertThrows(IllegalArgumentException.class, () -> requestService.submit(1L, "CUSTOMER", request));
    }

    @Test
    void approve_exam_createsPendingSchedulingAppointmentWithoutPrescription() {
        Request request = pendingRequest(1L, "exam");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentRepository.existsByRequestId(1L)).thenReturn(false);

        RequestResponse response = requestService.approve(1L);

        assertEquals("APPROVED", request.getStatus());
        assertEquals("APPROVED", response.getStatus());
        assertNull(request.getPrescription());
        assertNotNull(request.getUpdatedAt());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment created = captor.getValue();

        assertEquals("PENDING_SCHEDULING", created.getStatus());
        assertEquals(1L, created.getRequest().getId());
        assertEquals(5L, created.getCustomer().getId());
        assertNull(created.getScheduledAt());
        assertNull(created.getOptometrist());
    }

    @Test
    void approve_product_doesNotCreateAppointment() {
        Request request = pendingRequest(1L, "product");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response = requestService.approve(1L);

        assertEquals("APPROVED", request.getStatus());
        assertEquals("APPROVED", response.getStatus());
        assertNotNull(request.getUpdatedAt());
        verify(appointmentRepository, never()).save(any());
        verify(appointmentRepository, never()).existsByRequestId(any());
    }

    @Test
    void approve_examWithExistingAppointment_throwsWithoutUpdating() {
        Request request = pendingRequest(1L, "exam");
        request.setUpdatedAt(LocalDateTime.of(2026, 9, 15, 10, 0));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(appointmentRepository.existsByRequestId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> requestService.approve(1L));

        assertEquals("PENDING", request.getStatus());
        assertEquals(LocalDateTime.of(2026, 9, 15, 10, 0), request.getUpdatedAt());
        verify(requestRepository, never()).save(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void approve_alreadyApproved_doesNotTouchUpdatedAt() {
        Request request = pendingRequest(1L, "exam");
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 15, 10, 30);
        request.setStatus("APPROVED");
        request.setUpdatedAt(updatedAt);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> requestService.approve(1L));

        assertEquals("APPROVED", request.getStatus());
        assertEquals(updatedAt, request.getUpdatedAt());
        verify(requestRepository, never()).save(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void reject_pending_marksRejectedWithoutAppointment() {
        Request request = pendingRequest(1L, "exam");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response = requestService.reject(1L);

        assertEquals("REJECTED", request.getStatus());
        assertEquals("REJECTED", response.getStatus());
        assertNull(request.getPrescription());
        assertNotNull(request.getUpdatedAt());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void reject_alreadyRejected_doesNotTouchUpdatedAt() {
        Request request = pendingRequest(1L, "exam");
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 15, 11, 0);
        request.setStatus("REJECTED");
        request.setUpdatedAt(updatedAt);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> requestService.reject(1L));

        assertEquals("REJECTED", request.getStatus());
        assertEquals(updatedAt, request.getUpdatedAt());
        verify(requestRepository, never()).save(any());
    }
}