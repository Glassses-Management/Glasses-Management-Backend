package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrescriptionRequest {

    @NotNull(message = "Customer id is require")
    private Long customer_id;

    private Long user_id;

    private Float od_sphere;
    private Float od_cylinder;

    @Min(value = 0, message = "Axis must be between 0 and 180")
    @Max(value = 180, message = "Axis must be between 0 and 180")
    private Integer od_axis;

    private Float os_sphere;
    private Float os_cylinder;

    @Min(value = 0, message = "Axis must be between 0 and 180")
    @Max(value = 180, message = "Axis must be between 0 and 180")
    private Integer os_axis;

    private Float near_addition;

    @Min(value = 0, message = "Pupillary distance cannot be negative")
    private Float pupillary_distance;

    @Size(max = 255, message = "Notes must be under 255 character")
    private String notes;

    private LocalDate prescription_date;
}
