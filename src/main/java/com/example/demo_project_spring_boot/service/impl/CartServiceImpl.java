package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.model.Cart;
import com.example.demo_project_spring_boot.model.CartItem;
import com.example.demo_project_spring_boot.model.Product;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.CartRepository;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductReposity productReposity;
    private final UserRepository userRepository;


    @Override
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() ->{
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User Id" + userId + " not found"));


            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Override
    @Transactional
    public Cart addToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productReposity.findById(productId)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញផលិតផលទេ!"));

        // ឆែកមើលថាតើទំនិញនេះមានក្នុងកន្ត្រករួចហើយឬនៅ
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // បើមានហើយ គ្រាន់តែបូកចំនួនថែម
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            // បើមិនទាន់មាន បង្កើតថ្មី
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(product.getProPrice());

            // 🌟 បន្ទាត់នេះសំខាន់បំផុត! ត្រូវតែបន្ថែមដើម្បីកុំឱ្យវាចេញ <null> ទៀត
            newItem.setCart(cart);

            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }
    @Override
    public Cart updateItemQuantity(Long userId, Long productId, Integer newQuantity) {
        Cart cart = getOrCreateCart(userId);

        if (newQuantity <= 0) {
            return removeItemFromCart(userId , productId);
        }

        cart.getItems().stream()
                .filter(item -> item.getProduct().getProId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(newQuantity));
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProduct().getProId().equals(productId));
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPrice(Long userId) { // កែពី Double ទៅជា BigDecimal
        Cart cart = getOrCreateCart(userId);

        return cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
