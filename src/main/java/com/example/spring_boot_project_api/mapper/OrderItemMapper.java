package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.OrderItemResponse;
import com.example.spring_boot_project_api.model.OrderItem;

@Component
public class OrderItemMapper {

    public OrderItem toEntity(OrderItemRequest dto) {
        OrderItem orderItem = new OrderItem();

        orderItem.setQuantity(dto.getQuantity());
        orderItem.setLen_type(dto.getLen_type());
        orderItem.setLen_coating(dto.getLen_coating());
        orderItem.setLen_index(dto.getLen_index());
        orderItem.setLen_price(dto.getLen_price());

        return orderItem;
    }

    public OrderItemResponse toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        OrderItemResponse response = new OrderItemResponse();

        response.setId(orderItem.getId());
        response.setOrder_id(orderItem.getOrder() != null ? orderItem.getOrder().getId() : null);
        response.setProduct_id(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null);

        response.setQuantity(orderItem.getQuantity());

        response.setLen_type(orderItem.getLen_type());
        response.setLen_coating(orderItem.getLen_coating());
        response.setLen_index(orderItem.getLen_index());
        response.setLen_price(orderItem.getLen_price());

        response.setUnit_price(orderItem.getUnit_price());
        response.setTotal_price(orderItem.getTotal_price());

        return response;
    }
}
