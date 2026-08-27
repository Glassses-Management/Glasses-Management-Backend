package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Customer id is required")
    private Long customer_id;

    @NotEmpty(message = "Order must have at least one item")
    @Size(min = 1, message = "Order must have at least one item")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {

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

        private BigDecimal len_price;
    }
}
