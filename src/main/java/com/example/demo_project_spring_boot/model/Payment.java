package com.example.demo_project_spring_boot.model;

import com.example.demo_project_spring_boot.Enum.PaymentMethod;
import com.example.demo_project_spring_boot.Enum.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    private BigDecimal amount;
    
    private String transactionId;
    
    private String paymentGateway;
    
    @Column(columnDefinition = "TEXT")
    private String paymentDetails;
    
    private LocalDateTime paymentDate;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.paymentDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.PENDING;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
