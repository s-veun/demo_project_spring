package com.example.demo_project_spring_boot.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponsRequestDto {
    private String code;
    private Double discountValue;
    private LocalDateTime expiryDate;
    private Boolean active;
}