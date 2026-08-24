package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "SKU is require")
    @Size(max = 50, message = "SKU must be under 50 character")
    private String sku;

    @NotBlank(message = "Model is require")
    @Size(max = 100, message = "Model must be under 100 character")
    private String model;

    @NotBlank(message = "Brand is require")
    @Size(max = 50, message = "Brand must be under 50 character")
    private String brand;

    @Size(max = 30, message = "Color must be under 30 character")
    private String color;

    @Size(max = 50, message = "Material must be under 50 character")
    private String material;

    @Size(max = 20, message = "Size must be under 20 character")
    private String size;

    @NotNull(message = "Cost price is require")
    @Positive(message = "Cost price must be greater than 0")
    private Float cost_price;

    @NotNull(message = "Sale price is require")
    @Positive(message = "Sale price must be greater than 0")
    private Float sale_price;

    @Size(max = 100, message = "Supplier name must be under 100 character")
    private String supplier_name;

    @Size(max = 20, message = "Supplier contact must be under 20")
    private String supplier_contact;

    @NotBlank(message = "Category is require")
    @Size(max = 50, message = "Category must be under 50 character")
    private String category;
}
