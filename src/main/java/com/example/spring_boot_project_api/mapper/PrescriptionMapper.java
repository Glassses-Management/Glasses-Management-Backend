package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.PrescriptionRequest;
import com.example.spring_boot_project_api.dto.response.PrescriptionResponse;
import com.example.spring_boot_project_api.model.Prescription;

@Component
public class PrescriptionMapper {

    public Prescription toEntity(PrescriptionRequest dto) {
        Prescription prescription = new Prescription();

        prescription.setOd_sphere(dto.getOd_sphere());
        prescription.setOd_cylinder(dto.getOd_cylinder());
        prescription.setOd_axis(dto.getOd_axis());
        prescription.setOs_sphere(dto.getOs_sphere());
        prescription.setOs_cylinder(dto.getOs_cylinder());
        prescription.setOs_axis(dto.getOs_axis());
        prescription.setNear_addition(dto.getNear_addition());
        prescription.setPupillary_distance(dto.getPupillary_distance());
        prescription.setNotes(dto.getNotes());
        prescription.setPrescription_date(dto.getPrescription_date());

        return prescription;
    }

    public PrescriptionResponse toResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        PrescriptionResponse response = new PrescriptionResponse();

        response.setId(prescription.getId());
        response.setCustomer_id(prescription.getCustomer() != null ? prescription.getCustomer().getId() : null);
        response.setUser_id(prescription.getUser() != null ? prescription.getUser().getId() : null);
        response.setBranch_id(prescription.getBranch() != null ? prescription.getBranch().getId() : null);

        response.setOd_sphere(prescription.getOd_sphere());
        response.setOd_cylinder(prescription.getOd_cylinder());
        response.setOd_axis(prescription.getOd_axis());
        response.setOs_sphere(prescription.getOs_sphere());
        response.setOs_cylinder(prescription.getOs_cylinder());
        response.setOs_axis(prescription.getOs_axis());

        response.setNear_addition(prescription.getNear_addition());
        response.setPupillary_distance(prescription.getPupillary_distance());

        response.setNotes(prescription.getNotes());

        response.setPrescription_date(prescription.getPrescription_date());
        response.setCreated_at(prescription.getCreated_at());

        return response;
    }
}
