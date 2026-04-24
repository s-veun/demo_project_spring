package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.dto.OrderRequestDto; // 🌟 ទាមទារ DTO សម្រាប់ Coupon
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.*;
import com.example.demo_project_spring_boot.repository.CouponRepository; // 🌟 សម្រាប់ឆែក Coupon
import com.example.demo_project_spring_boot.repository.OrderRepository;
import com.example.demo_project_spring_boot.repository.ProductReposity;
import com.example.demo_project_spring_boot.service.CartService;
import com.example.demo_project_spring_boot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductReposity productReposity;
    private final CartService cartService;
    private final CouponRepository couponRepository; // កុំភ្លេចបន្ថែមវា

    // ==========================================
    // ១. មុខងារបញ្ជាទិញ (ភ្ជាប់ Coupon + កាត់ស្តុក)
    // ==========================================
    @Override
    @Transactional
    public Order placeOrder(OrderRequestDto requestDto) { // 🌟 ប្រើ OrderRequestDto
        Long userId = requestDto.getUserId();
        Cart cart = cartService.getOrCreateCart(userId);

        // ១. ឆែកមើលថាកន្ត្រកទទេឬអត់
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        // ២. គិតលុយ
        BigDecimal subTotal = cartService.calculateTotalPrice(userId);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        // ៣. ឆែកកូដបញ្ចុះតម្លៃ (បើមាន)
        if (requestDto.getCouponCode() != null && !requestDto.getCouponCode().trim().isEmpty()) {
            appliedCoupon = couponRepository.findByCode(requestDto.getCouponCode().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("រកមិនឃើញកូដបញ្ចុះតម្លៃនេះទេ!"));

            if (!appliedCoupon.getActive()) {
                throw new RuntimeException("កូដបញ្ចុះតម្លៃនេះត្រូវបានបិទលែងឱ្យប្រើប្រាស់ហើយ!");
            }
            if (appliedCoupon.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("កូដបញ្ចុះតម្លៃនេះបានផុតកំណត់ហើយ!");
            }
            if (orderRepository.existsByUser_IdAndCoupon_CouponId(userId, appliedCoupon.getCouponId())) {
                throw new RuntimeException("អ្នកបានប្រើប្រាស់កូដបញ្ចុះតម្លៃនេះរួចរាល់ហើយ! (Coupon used)");
            }

            discountAmount = BigDecimal.valueOf(appliedCoupon.getDiscountValue());
        }

        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
            discountAmount = subTotal;
        }

        // ៤. បង្កើត Order ថ្មី
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setSubTotal(subTotal);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setCoupon(appliedCoupon);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderItems(new ArrayList<>());

        // ៥. កាត់ស្តុក និងបញ្ចូល OrderItem
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()){
                throw new RuntimeException("Product quantity is less than order quantity: " + product.getProName());
            }

            // កាត់ស្តុកចេញពីឃ្លាំង
            product.setStock(product.getStock() - cartItem.getQuantity());
            productReposity.save(product);

            // បង្កើត Item សម្រាប់វិក្កយបត្រ
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getUnitPrice()) // តម្រូវតាមកូដដើមរបស់អ្នក
                    .build();

            order.getOrderItems().add(orderItem);
        }

        // ៦. រក្សាទុកវិក្កយបត្រ
        Order savedOrder = orderRepository.save(order);

        // ៧. លុបសម្អាតកន្ត្រក
        cartService.clearCart(userId);

        return savedOrder;
    }

    // ==========================================
    // ២. មុខងារ Update ស្ថានភាព (Admin)
    // ==========================================
    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("រកមិនឃើញវិក្កយបត្រលេខ #" + orderId + " ទេ!"));

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ==========================================
    // ៣. មុខងារលុបចោលការបញ្ជាទិញ (Cancel Order)
    // ==========================================
    @Override
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("រកមិនឃើញវិក្កយបត្រលេខ #" + orderId + " ទេ!"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("វិក្កយបត្រនេះត្រូវបានលុបចោលរួចហើយ!");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("អ្នកអាចលុបចោលបានតែវិក្កយបត្រដែលកំពុង PENDING ប៉ុណ្ណោះ!");
        }

        // លំហូរបង្វិលស្តុកចូលឃ្លាំងវិញ (Refund Stock)
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productReposity.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}