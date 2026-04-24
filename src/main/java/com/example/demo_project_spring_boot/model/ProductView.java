package com.example.demo_project_spring_boot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_views", indexes = {
        @Index(name = "idx_product_viewed_at", columnList = "viewedAt"),
        @Index(name = "idx_product_view_user", columnList = "user_id"),
        @Index(name = "idx_product_view_product", columnList = "product_id")
})
public class ProductView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Can be null for anonymous users

    @Column(nullable = false)
    private String sessionId; // Track anonymous sessions

    private String ipAddress;
    private String userAgent;
    private String referrer; // Where they came from

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    private Integer viewDuration; // In seconds (if tracked)

    @PrePersist
    protected void onCreate() {
        this.viewedAt = LocalDateTime.now();
    }
}
