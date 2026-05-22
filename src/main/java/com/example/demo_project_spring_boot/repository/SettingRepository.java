package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    Optional<Setting> findBySettingKeyAndTenantKeyAndLocaleKey(String settingKey, String tenantKey, String localeKey);

    List<Setting> findByCategoryAndTenantKeyAndLocaleKey(String category, String tenantKey, String localeKey);

    List<Setting> findBySettingKeyInAndTenantKeyAndLocaleKey(Collection<String> keys, String tenantKey, String localeKey);

    List<Setting> findByTenantKeyAndLocaleKey(String tenantKey, String localeKey);
}

