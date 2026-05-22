package com.example.demo_project_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "setting_audit_logs", indexes = {
        @Index(name = "idx_setting_audit_key", columnList = "setting_key"),
        @Index(name = "idx_setting_audit_updated_at", columnList = "updated_at")
})
public class SettingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, length = 200)
    private String settingKey;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "tenant_key", nullable = false, length = 80)
    private String tenantKey;

    @Column(name = "locale_key", nullable = false, length = 20)
    private String localeKey;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
        if (this.tenantKey == null || this.tenantKey.isBlank()) {
            this.tenantKey = "default";
        }
        if (this.localeKey == null || this.localeKey.isBlank()) {
            this.localeKey = "en";
        }
    }
}

