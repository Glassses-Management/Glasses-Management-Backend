package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.PrescriptionRequest;
import com.example.spring_boot_project_api.dto.response.PrescriptionResponse;
import com.example.spring_boot_project_api.service.PrescriptionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<PrescriptionResponse>> createBulk(@Valid @RequestBody List<@Valid PrescriptionRequest> requests) {
        List<PrescriptionResponse> saved = requests.stream()
                .map(prescriptionService::create)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<PrescriptionResponse> getAll() {
        return prescriptionService.getAll();
    }

    /**
     * The signed-in customer's own prescriptions. Replaces the public
     * /prescriptions/customer/{customerId} route for customer pages, which let any
     * signed-in user read another patient's medical data by changing the id.
     */
    @GetMapping("/mine")
    public List<PrescriptionResponse> getMine(Authentication authentication) {
        return prescriptionService.getMine(authentication);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse getById(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<PrescriptionResponse> getByCustomerId(@PathVariable Long customerId) {
        return prescriptionService.getByCustomerId(customerId);
    }

    @GetMapping("/user/{userId}")
    public List<PrescriptionResponse> getByUserId(@PathVariable Long userId) {
        return prescriptionService.getByUserId(userId);
    }

    @PutMapping("/{id}")
    public PrescriptionResponse update(@PathVariable Long id, @Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        prescriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
