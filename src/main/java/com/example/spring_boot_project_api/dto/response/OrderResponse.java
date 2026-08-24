package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private Long customer_id;
    private Long user_id;
    private Long branch_id;
    private Long prescription_id;
    private Long product_id;

    private Integer quantity;
    private Float unit_price;
    private Float total_amount;

    private String status;
    private String payment_method;
    private String payment_status;

    private LocalDate order_date;
    private LocalDateTime created_at;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updated_at;
}
