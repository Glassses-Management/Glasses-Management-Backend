package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Customer id is require")
    private Long customer_id;

    private Long user_id;

    @NotNull(message = "Branch id is require")
    private Long branch_id;

    @NotNull(message = "Precription id is require")
    private Long prescription_id;

    @NotNull(message = "Product id is require")
    private Long product_id;

    @NotNull(message = "Quantity is require")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 20, message = "Status must be under 20 character")
    private String status;

    @Size(max = 30, message = "Payment method must be under 30 character")
    private String payment_method;

    @Size(max = 20, message = "Payment status must be under 20 character")
    private String payment_status;
}
