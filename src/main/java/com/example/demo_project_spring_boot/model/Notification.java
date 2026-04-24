package com.example.demo_project_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String type; // ORDER_UPDATE, PROMOTION, SYSTEM, etc.
    
    private String referenceType;
    
    private String referenceId;
    
    private Boolean isRead;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime readAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.isRead == null) this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
    
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
