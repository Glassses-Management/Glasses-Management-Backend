package com.example.spring_boot_project_api.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.model.Order;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setCustomer_id(order.getCustomer() != null ? order.getCustomer().getId() : null);
        response.setTotal(order.getTotal());
        response.setStatus(order.getStatus());
        response.setPayment_method(order.getPayment_method());
        response.setPayment_status(order.getPayment_status());
        response.setOrder_date(order.getOrder_date());
        response.setCreated_at(order.getCreated_at());
        response.setUpdated_at(order.getUpdated_at());

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                    .map(orderItemMapper::toResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setItems(Collections.emptyList());
        }

        return response;
    }
}
