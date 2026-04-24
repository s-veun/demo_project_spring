package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.ProductResponseDTO;
import com.example.demo_project_spring_boot.model.Wishlist;

import java.util.List;

public interface WishlistService {
    Wishlist addToWishlist(Long userId, Long productId);
    void removeFromWishlist(Long userId, Long productId);
    List<Wishlist> getUserWishlist(Long userId);
    boolean isInWishlist(Long userId, Long productId);
    void clearWishlist(Long userId);
}
