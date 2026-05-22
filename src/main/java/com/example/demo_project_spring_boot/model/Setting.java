package com.example.demo_project_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_settings_key_tenant_locale", columnNames = {"setting_key", "tenant_key", "locale_key"})
        },
        indexes = {
        @Index(name = "idx_settings_category", columnList = "category"),
        @Index(name = "idx_settings_tenant_locale", columnList = "tenant_key, locale_key")
})
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, length = 200)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Builder.Default
    @Column(name = "tenant_key", nullable = false, length = 80)
    private String tenantKey = "default";

    @Builder.Default
    @Column(name = "locale_key", nullable = false, length = 20)
    private String localeKey = "en";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.tenantKey == null || this.tenantKey.isBlank()) {
            this.tenantKey = "default";
        }
        if (this.localeKey == null || this.localeKey.isBlank()) {
            this.localeKey = "en";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


