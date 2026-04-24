package com.example.demo_project_spring_boot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class CartResponseDto {
    private Long cartId;
    private Long userId;
    private List<CartItemResponseDto> items;
    private BigDecimal totalPrice;
}
