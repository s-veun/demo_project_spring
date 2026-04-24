package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.CartItemResponseDto;
import com.example.demo_project_spring_boot.dto.CartResponseDto;
import com.example.demo_project_spring_boot.model.Cart;
import com.example.demo_project_spring_boot.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ១. ទាញយកកន្ត្រកទំនិញរបស់អ្នកប្រើប្រាស់
    @GetMapping("/{userId}")
    @Transactional
    public ResponseEntity<CartResponseDto> getCart(@PathVariable Long userId) {
        Cart cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ២. បន្ថែមទំនិញចូលកន្ត្រក
    @PostMapping("/{userId}/add")
    @Transactional
    public ResponseEntity<CartResponseDto> addToCart(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {

        Cart cart = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៣. កែប្រែចំនួនទំនិញ
    @PutMapping("/{userId}/update")
    @Transactional
    public ResponseEntity<CartResponseDto> updateQuantity(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {

        Cart cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៤. លុបទំនិញណាមួយចេញពីកន្ត្រក
    @DeleteMapping("/{userId}/remove/{productId}")
    @Transactional
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {

        Cart cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(mapToCartResponseDto(cart, userId));
    }

    // ៥. សម្អាតកន្ត្រកចោលទាំងអស់
    @DeleteMapping("/{userId}/clear")
    @Transactional
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
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

        // បំប្លែង CartItem ទៅជា CartItemResponseDto
        List<CartItemResponseDto> itemDtos = cart.getItems().stream().map(item -> {
            CartItemResponseDto itemDto = new CartItemResponseDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct().getProId());

            // សន្មត់ថា Product របស់អ្នកមាន getProductName() ឬ getName()
            // សូមកែប្រែឈ្មោះ Method នេះទៅតាម Entity Product ជាក់ស្តែងរបស់អ្នក
            itemDto.setProductName(item.getProduct().getProName());

            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());

            // គណនាតម្លៃសរុបរាយ (subTotal = quantity * unitPrice)
            BigDecimal subTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemDto.setSubTotal(subTotal);

            return itemDto;
        }).collect(Collectors.toList());

        responseDto.setItems(itemDtos);

        // ហៅមុខងារគណនាតម្លៃសរុបពី Service មកបង្ហាញ
        responseDto.setTotalPrice(cartService.calculateTotalPrice(userId));

        return responseDto;
    }
}