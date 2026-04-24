package com.example.demo_project_spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponResponseDto {
    private Long couponId;
    private String code;
    private Double discountValue;
    private LocalDateTime expiryDate;
    private Boolean active;
}
