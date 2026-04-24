package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.model.Wishlist;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.repository.WishlistRepository;
import com.example.demo_project_spring_boot.service.WishlistService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductReposity productReposity;

    @Override
    public Wishlist addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (wishlistRepository.existsByUserIdAndProductProId(userId, productId)) {
            throw new RuntimeException("Product already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        return wishlistRepository.save(wishlist);
    }

    @Override
    public void removeFromWishlist(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductProId(userId, productId)) {
            throw new ResourceNotFoundException("Wishlist item not found");
        }
        wishlistRepository.deleteByUserIdAndProductProId(userId, productId);
    }

    @Override
    public List<Wishlist> getUserWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }

    @Override
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductProId(userId, productId);
    }

    @Override
    public void clearWishlist(Long userId) {
        List<Wishlist> items = wishlistRepository.findByUserId(userId);
        wishlistRepository.deleteAll(items);
    }
}
