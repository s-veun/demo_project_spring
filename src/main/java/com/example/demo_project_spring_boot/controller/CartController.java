package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.CartItemResponseDto;
import com.example.demo_project_spring_boot.dto.CartResponseDto;
import com.example.demo_project_spring_boot.exception.ForbiddenException;
import com.example.demo_project_spring_boot.exception.UnauthorizedException;
import com.example.demo_project_spring_boot.model.Cart;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    // =====================================================================
    // Security helper – validate the authenticated principal can access
    // the requested userId cart. ADMINs are allowed all carts; USERs only
    // their own.
    // =====================================================================
    private void validateCartAccess(Long requestedUserId, UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("[CartController] Cart access attempted with no authentication for userId: {}", requestedUserId);
            throw new UnauthorizedException("Authentication required to access cart");
        }
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            log.debug("[CartController] ADMIN '{}' accessing cart for userId: {}", userDetails.getUsername(), requestedUserId);
            return;
        }
        // Regular user – must own the cart
        User dbUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
        if (!dbUser.getId().equals(requestedUserId)) {
            log.warn("[CartController] User '{}' (id={}) attempted to access cart of userId: {}",
                    userDetails.getUsername(), dbUser.getId(), requestedUserId);
            throw new ForbiddenException("You are not allowed to access another user's cart");
        }
        log.debug("[CartController] User '{}' (id={}) accessing their own cart", userDetails.getUsername(), dbUser.getId());
    }

    // ១. ទាញយកកន្ត្រកទំនិញរបស់អ្នកប្រើប្រាស់
    @GetMapping("/{userId}")
    @Transactional
    @Operation(summary = "Get user's shopping cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – valid Bearer token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden – users can only access their own cart")
    })
    public ResponseEntity<CartResponseDto> getCart(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateCartAccess(userId, userDetails);
        Cart cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ២. បន្ថែមទំនិញចូលកន្ត្រក
    @PostMapping("/{userId}/add")
    @Transactional
    @Operation(summary = "Add product to cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product added to cart successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – valid Bearer token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden – users can only access their own cart"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<CartResponseDto> addToCart(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            @RequestParam
            @Parameter(description = "Product ID to add", required = true)
            Long productId,
            @RequestParam(defaultValue = "1")
            @Parameter(description = "Quantity to add (default: 1)")
            Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateCartAccess(userId, userDetails);
        Cart cart = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៣. កែប្រែចំនួនទំនិញ
    @PutMapping("/{userId}/update")
    @Transactional
    @Operation(summary = "Update product quantity in cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – valid Bearer token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden – users can only access their own cart"),
            @ApiResponse(responseCode = "404", description = "Product not found in cart")
    })
    public ResponseEntity<CartResponseDto> updateQuantity(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            @RequestParam
            @Parameter(description = "Product ID", required = true)
            Long productId,
            @RequestParam
            @Parameter(description = "New quantity", required = true)
            Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateCartAccess(userId, userDetails);
        Cart cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៤. លុបទំនិញណាមួយចេញពីកន្ត្រក
    @DeleteMapping("/{userId}/remove/{productId}")
    @Transactional
    @Operation(summary = "Remove product from cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product removed from cart successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – valid Bearer token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden – users can only access their own cart"),
            @ApiResponse(responseCode = "404", description = "Product not found in cart")
    })
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            @PathVariable
            @Parameter(description = "Product ID to remove", required = true)
            Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateCartAccess(userId, userDetails);
        Cart cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៥. សម្អាតកន្ត្រកចោលទាំងអស់
    @DeleteMapping("/{userId}/clear")
    @Transactional
    @Operation(summary = "Clear all items from cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – valid Bearer token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden – users can only access their own cart")
    })
    public ResponseEntity<String> clearCart(
            @PathVariable
            @Parameter(description = "User ID", required = true)
            Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateCartAccess(userId, userDetails);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart has been cleared");
    }

    // =====================================================================
    // Helper Method សម្រាប់បំប្លែង Cart (Entity) ទៅជា CartResponseDto (DTO)
    // =====================================================================
    private CartResponseDto mapToCartResponseDto(Cart cart, Long userId) {
        CartResponseDto responseDto = new CartResponseDto();
        responseDto.setCartId(cart.getCartId());
        responseDto.setUserId(userId);

        List<CartItemResponseDto> itemDtos = cart.getItems().stream().map(item -> {
            CartItemResponseDto itemDto = new CartItemResponseDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct().getProId());
            itemDto.setProductName(item.getProduct().getProName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            BigDecimal subTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemDto.setSubTotal(subTotal);
            return itemDto;
        }).collect(Collectors.toList());

        responseDto.setItems(itemDtos);
        responseDto.setTotalPrice(cartService.calculateTotalPrice(userId));

        return responseDto;
    }
}