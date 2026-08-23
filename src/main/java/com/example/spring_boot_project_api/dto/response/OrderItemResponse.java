package com.example.spring_boot_project_api.dto.response;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long id;
    private Long order_id;
    private Long product_id;

    private Integer quantity;

    private String len_type;
    private String len_coating;
    private Float len_index;
    private Float len_price;

    private Float unit_price;
    private Float total_price;
}
