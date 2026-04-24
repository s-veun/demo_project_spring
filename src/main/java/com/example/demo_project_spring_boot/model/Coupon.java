package com.example.demo_project_spring_boot.model;

import com.example.demo_project_spring_boot.Enum.DiscountType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "coupons")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String code;
    
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    
    private Double discountValue;
    
    private Double minOrderAmount;
    
    private Double maxDiscountAmount;
    
    private Integer usageLimit;
    
    private Integer usedCount;
    
    private Boolean active;
    
    private LocalDateTime startDate;
    
    private LocalDateTime expiryDate;
    
    @OneToMany(mappedBy = "coupon")
    @JsonIgnore
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.active == null) this.active = true;
        if (this.usedCount == null) this.usedCount = 0;
        if (this.discountType == null) this.discountType = DiscountType.PERCENTAGE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }
    
    public boolean isUsageLimitReached() {
        return usageLimit != null && usedCount >= usageLimit;
    }
    
    public boolean isValid() {
        return active && !isExpired() && !isUsageLimitReached();
    }
}
