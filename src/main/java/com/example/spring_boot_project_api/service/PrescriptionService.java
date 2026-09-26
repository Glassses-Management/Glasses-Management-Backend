package com.example.spring_boot_project_api.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.PrescriptionRequest;
import com.example.spring_boot_project_api.dto.response.PrescriptionResponse;
import com.example.spring_boot_project_api.mapper.PrescriptionMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Prescription;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.PrescriptionRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PrescriptionMapper prescriptionMapper;
    private final CustomerAccessService customerAccessService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               CustomerRepository customerRepository,
                               UserRepository userRepository,
                               PrescriptionMapper prescriptionMapper,
                               CustomerAccessService customerAccessService){
        this.prescriptionRepository = prescriptionRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.prescriptionMapper = prescriptionMapper;
        this.customerAccessService = customerAccessService;
    }

    /**
     * Prescriptions belonging to the signed-in customer, resolved from the token
     * instead of a customerId path variable. Newest first.
     *
     * Not read-only: resolving the profile may backfill a missing user_id link.
     */
    @Transactional
    public List<PrescriptionResponse> getMine(Authentication authentication){
        Customer customer = customerAccessService.findMyCustomer(authentication);
        if (customer == null) {
            return List.of();
        }

        return prescriptionRepository.findByCustomerId(customer.getId()).stream()
                .sorted(Comparator.comparing(Prescription::getId).reversed())
                .map(prescriptionMapper::toResponse)
                .toList();
    }

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request){
        Customer customer = customerRepository.findById(request.getCustomer_id())
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        User user = resolveUser(request.getUser_id());

        Prescription prescription = prescriptionMapper.toEntity(request);
        prescription.setCustomer(customer);
        prescription.setUser(user);

        Prescription saved = prescriptionRepository.save(prescription);
        PrescriptionResponse response = prescriptionMapper.toResponse(saved);
        response.setUpdatedAt(null);

        return response;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getAll(){
        return prescriptionRepository.findAll().stream().map(prescriptionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Long id){
        return prescriptionMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getByCustomerId(Long customerId){
        return prescriptionRepository.findByCustomerId(customerId).stream()
                .map(prescriptionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getByUserId(Long userId){
        return prescriptionRepository.findByUserId(userId).stream()
                .map(prescriptionMapper::toResponse).toList();
    }

    @Transactional
    public PrescriptionResponse update(Long id, PrescriptionRequest request){
        Prescription prescription = getEntity(id);

        Customer customer = customerRepository.findById(request.getCustomer_id())
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        User user = resolveUser(request.getUser_id());

        prescription.setOd_sphere(request.getOd_sphere());
        prescription.setOd_cylinder(request.getOd_cylinder());
        prescription.setOd_axis(request.getOd_axis());
        prescription.setOs_sphere(request.getOs_sphere());
        prescription.setOs_cylinder(request.getOs_cylinder());
        prescription.setOs_axis(request.getOs_axis());
        prescription.setNear_addition(request.getNear_addition());
        prescription.setPupillary_distance(request.getPupillary_distance());
        prescription.setNotes(request.getNotes());
        prescription.setPrescription_date(request.getPrescription_date());

        prescription.setCustomer(customer);
        prescription.setUser(user);

        Prescription saved = prescriptionRepository.saveAndFlush(prescription);
        PrescriptionResponse response = prescriptionMapper.toResponse(saved);
        response.setCreatedAt(null);

        return response;
    }

    public void delete(Long id){
        prescriptionRepository.delete(getEntity(id));
    }

    private User resolveUser(Long userId){
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
               .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Prescription getEntity(Long id){
        return prescriptionRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Prescription not found"));
    }
}
