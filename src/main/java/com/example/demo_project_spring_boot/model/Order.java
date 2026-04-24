package com.example.demo_project_spring_boot.model;

import com.example.demo_project_spring_boot.Enum.OrderStatus;
import com.example.demo_project_spring_boot.Enum.PaymentMethod;
import com.example.demo_project_spring_boot.Enum.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tbl_orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private LocalDateTime orderDate;
    
    private LocalDateTime paymentDate;
    
    private LocalDateTime shippingDate;
    
    private LocalDateTime deliveredDate;
    
    private LocalDateTime updatedAt;

    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal tax;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    @JsonIgnore
    private Coupon coupon;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;
    
    private String trackingNumber;
    
    private String shippingCarrier;
    
    @Column(columnDefinition = "TEXT")
    private String orderNotes;
    
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
        if (this.status == null) this.status = OrderStatus.PENDING;
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}