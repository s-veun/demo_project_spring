package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.SettingAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingAuditLogRepository extends JpaRepository<SettingAuditLog, Long> {

    Page<SettingAuditLog> findByTenantKeyAndLocaleKeyOrderByUpdatedAtDesc(String tenantKey, String localeKey, Pageable pageable);
}

