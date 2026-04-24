package com.example.demo_project_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity(name = "inventory_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
    
    private Integer previousStock;
    
    private Integer newStock;
    
    private Integer stockChange;
    
    private String reason;
    
    private String referenceType; // ORDER, RESTOCK, ADJUSTMENT, etc.
    
    private String referenceId;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.stockChange == null) {
            this.stockChange = this.newStock - this.previousStock;
        }
    }
}
