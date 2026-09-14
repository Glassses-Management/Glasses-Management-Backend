package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.RequestRequest;
import com.example.spring_boot_project_api.dto.response.RequestResponse;
import com.example.spring_boot_project_api.model.Request;

@Component
public class RequestMapper {

    public Request toEntity(RequestRequest dto) {
        Request request = new Request();
        request.setType(dto.getType());
        request.setNotes(dto.getNotes());
        return request;
    }

    public RequestResponse toResponse(Request request) {
        if (request == null) {
            return null;
        }
        RequestResponse response = new RequestResponse();
        response.setId(request.getId());
        response.setCustomer_id(request.getCustomer() != null ? request.getCustomer().getId() : null);
        response.setCustomer_name(request.getCustomer() != null ? request.getCustomer().getName() : null);
        response.setType(request.getType());
        response.setStatus(request.getStatus());
        response.setNotes(request.getNotes());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }
}
