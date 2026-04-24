package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.model.Cart;
import java.math.BigDecimal; // កុំភ្លេច Import ផង

public interface CartService {

    Cart getOrCreateCart(Long userId); // 💡 កែ cartId ទៅជា userId

    Cart addToCart(Long userId, Long productId, Integer quantity);

    Cart updateItemQuantity(Long userId, Long productId, Integer newQuantity);

    Cart removeItemFromCart(Long userId, Long productId);

    void clearCart(Long userId);

    BigDecimal calculateTotalPrice(Long userId); // 💡 កែពី double ទៅជា BigDecimal
}