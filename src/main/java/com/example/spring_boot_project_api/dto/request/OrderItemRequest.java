package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "Order id is required")
    private Long order_id;

    @NotNull(message = "Product id is required")
    private Long product_id;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 50, message = "Lens type must be under 50 character")
    private String len_type;

    @Size(max = 50, message = "Lens coating must be under 50 character")
    private String len_coating;

    @Size(max = 50, message = "Lens index must be under 50 character")
    private String len_index;

    @DecimalMin(value = "0.0", inclusive = false, message = "Lens price must be greater than 0")
    private BigDecimal len_price;
}
