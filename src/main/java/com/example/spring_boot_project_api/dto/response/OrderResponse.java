package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private Long customer_id;
    private String customer_name;
    private List<OrderItemResponse> items;
    private BigDecimal total;
    private String status;
    private String request_type;
    private String notes;
    private String payment_method;
    private String payment_status;
    private LocalDate order_date;
    private LocalDateTime created_at;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updated_at;
}
