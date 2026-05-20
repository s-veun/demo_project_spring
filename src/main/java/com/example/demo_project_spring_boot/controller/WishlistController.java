package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.model.Wishlist;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Tag(name = "Wishlist", description = "Wishlist management APIs")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @PostMapping("/add/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<?> addToWishlist(Authentication authentication, @PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId(authentication);
            Wishlist wishlist = wishlistService.addToWishlist(userId, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Product added to wishlist",
                    "wishlistId", wishlist.getWishlistId(),
                    "productId", productId
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<?> removeFromWishlist(Authentication authentication, @PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId(authentication);
            wishlistService.removeFromWishlist(userId, productId);
            return ResponseEntity.ok(Map.of("message", "Product removed from wishlist"));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get current user's wishlist")
    public ResponseEntity<?> getUserWishlist(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<Wishlist> wishlist = wishlistService.getUserWishlist(userId);
            return ResponseEntity.ok(Map.of(
                    "count", wishlist.size(),
                    "wishlist", wishlist
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<?> isInWishlist(Authentication authentication, @PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId(authentication);
            boolean inWishlist = wishlistService.isInWishlist(userId, productId);
            return ResponseEntity.ok(Map.of("inWishlist", inWishlist, "productId", productId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Clear entire wishlist")
    public ResponseEntity<?> clearWishlist(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            wishlistService.clearWishlist(userId);
            return ResponseEntity.ok(Map.of("message", "Wishlist cleared successfully"));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Extract the authenticated user's ID from the JWT principal.
     * Throws UnauthorizedException if the principal cannot be resolved.
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication required to access wishlist");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
        log.debug("[WishlistController] Resolved userId={} for username={}", user.getId(), username);
        return user.getId();
    }
}
