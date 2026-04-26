package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.model.Wishlist;
import com.example.demo_project_spring_boot.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> addToWishlist(Authentication authentication, @PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId(authentication);
            Wishlist wishlist = wishlistService.addToWishlist(userId, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Product added to wishlist",
                    "wishlist", wishlist
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> removeFromWishlist(Authentication authentication, @PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId(authentication);
            wishlistService.removeFromWishlist(userId, productId);
            return ResponseEntity.ok(Map.of("message", "Product removed from wishlist"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getUserWishlist(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<Wishlist> wishlist = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(Map.of(
                "count", wishlist.size(),
                "wishlist", wishlist
        ));
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> isInWishlist(Authentication authentication, @PathVariable Long productId) {
        Long userId = getCurrentUserId(authentication);
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> clearWishlist(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(Map.of("message", "Wishlist cleared"));
    }

    private Long getCurrentUserId(Authentication authentication) {
        // TODO: Implement proper user ID extraction from JWT token
        // For now, this is a placeholder - you'll need to extract user ID from your JWT
        return 1L; // Replace with actual user ID extraction
    }
}
