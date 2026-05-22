package com.example.demo_project_spring_boot.service.admin;

import com.example.demo_project_spring_boot.dto.admin.settings.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SettingsManagementService {

    GeneralSettingsDto getGeneralSettings(String tenant, String locale);

    GeneralSettingsDto updateGeneralSettings(GeneralSettingsDto request, String updatedBy, String tenant, String locale);

    SecuritySettingsDto getSecuritySettings(String tenant, String locale);

    SecuritySettingsDto updateSecuritySettings(SecuritySettingsDto request, String updatedBy, String tenant, String locale);

    EmailSettingsDto getEmailSettings(String tenant, String locale);

    EmailSettingsDto updateEmailSettings(EmailSettingsDto request, String updatedBy, String tenant, String locale);

    void sendTestEmail(TestEmailRequest request, String tenant, String locale);

    PaymentSettingsDto getPaymentSettings(String tenant, String locale);

    PaymentSettingsDto updatePaymentSettings(PaymentSettingsDto request, String updatedBy, String tenant, String locale);

    NotificationSettingsDto getNotificationSettings(String tenant, String locale);

    NotificationSettingsDto updateNotificationSettings(NotificationSettingsDto request, String updatedBy, String tenant, String locale);

    SeoSettingsDto getSeoSettings(String tenant, String locale);

    SeoSettingsDto updateSeoSettings(SeoSettingsDto request, String updatedBy, String tenant, String locale);

    SocialSettingsDto getSocialSettings(String tenant, String locale);

    SocialSettingsDto updateSocialSettings(SocialSettingsDto request, String updatedBy, String tenant, String locale);

    ThemeSettingsDto getThemeSettings(String tenant, String locale);

    ThemeSettingsDto updateThemeSettings(ThemeSettingsDto request, String updatedBy, String tenant, String locale);

    UploadAssetResponse uploadLogo(MultipartFile file, String updatedBy, String tenant, String locale);

    UploadAssetResponse uploadFavicon(MultipartFile file, String updatedBy, String tenant, String locale);

    Map<String, Object> getSystemInfo();

    Map<String, Object> getHealth();

    SettingsBackupResponse backupSettings(String tenant, String locale);

    void restoreSettings(SettingsRestoreRequest request, String updatedBy, String tenant, String locale);

    void importSettingsJson(MultipartFile file, String updatedBy, String tenant, String locale);
}

