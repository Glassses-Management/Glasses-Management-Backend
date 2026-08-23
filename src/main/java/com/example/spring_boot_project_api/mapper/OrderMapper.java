package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.model.Order;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest dto) {
        Order order = new Order();

        order.setQuantity(dto.getQuantity());
        order.setStatus(dto.getStatus());
        order.setPayment_method(dto.getPayment_method());
        order.setPayment_status(dto.getPayment_status());

        return order;
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setCustomer_id(order.getCustomer() != null ? order.getCustomer().getId() : null);
        response.setUser_id(order.getUser() != null ? order.getUser().getId() : null);
        response.setBranch_id(order.getBranch() != null ? order.getBranch().getId() : null);
        response.setPrescription_id(order.getPrescription() != null ? order.getPrescription().getId() : null);
        response.setProduct_id(order.getProduct() != null ? order.getProduct().getId() : null);

        response.setQuantity(order.getQuantity());
        response.setUnit_price(order.getUnit_price());
        response.setTotal_amount(order.getTotal_amount());

        response.setStatus(order.getStatus());
        response.setPayment_method(order.getPayment_method());
        response.setPayment_status(order.getPayment_status());

        response.setOrder_date(order.getOrder_date());
        response.setCreated_at(order.getCreated_at());

        return response;
    }
}
