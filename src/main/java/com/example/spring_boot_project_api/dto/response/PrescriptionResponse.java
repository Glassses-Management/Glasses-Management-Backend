package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PrescriptionResponse {
    private Long id;
    private Long customer_id;
    private Long user_id;
    private Long branch_id;

    private Float od_sphere;
    private Float od_cylinder;
    private Integer od_axis;

    private Float os_sphere;
    private Float os_cylinder;
    private Integer os_axis;

    private Float near_addition;
    private Float pupillary_distance;

    private String notes;

    private LocalDate prescription_date;
    private LocalDateTime created_at;
}
