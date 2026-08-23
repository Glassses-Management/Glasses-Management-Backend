package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "Order id is require")
    private Long order_id;

    @NotNull(message = "Product id is require")
    private Long product_id;

    @NotNull(message = "Quantity is require")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 50, message = "Lens type must be under 50 character")
    private String len_type;

    @Size(max = 50, message = "Lens coating must be under 50 character")
    private String len_coating;

    @DecimalMin(value = "1.0", message = "Lens index must be at least 1.0")
    private Float len_index;

    @Positive(message = "Lens price must be greater than 0")
    private Float len_price;
}
