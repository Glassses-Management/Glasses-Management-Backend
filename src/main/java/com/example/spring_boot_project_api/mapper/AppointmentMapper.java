package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.AppointmentRequest;
import com.example.spring_boot_project_api.dto.response.AppointmentResponse;
import com.example.spring_boot_project_api.model.Appointment;

@Component
public class AppointmentMapper {

    public Appointment toEntity(AppointmentRequest dto) {
        Appointment appointment = new Appointment();

        appointment.setScheduledAt(dto.getScheduled_at());
        appointment.setStatus(dto.getStatus());
        appointment.setNotes(dto.getNotes());

        return appointment;
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();

        response.setId(appointment.getId());
        response.setCustomer_id(appointment.getCustomer() != null ? appointment.getCustomer().getId() : null);
        response.setBranch_id(appointment.getBranch() != null ? appointment.getBranch().getId() : null);
        response.setOptometrist_id(appointment.getOptometrist() != null ? appointment.getOptometrist().getId() : null);
        response.setScheduled_at(appointment.getScheduledAt());
        response.setStatus(appointment.getStatus());
        response.setNotes(appointment.getNotes());

        return response;
    }
}
