package com.example.demo_project_spring_boot.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
