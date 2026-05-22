package com.example.demo_project_spring_boot.service.admin.impl;

import com.example.demo_project_spring_boot.config.SettingsUploadProperties;
import com.example.demo_project_spring_boot.dto.admin.settings.*;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import com.example.demo_project_spring_boot.mapper.SettingsMapper;
import com.example.demo_project_spring_boot.model.Setting;
import com.example.demo_project_spring_boot.model.SettingAuditLog;
import com.example.demo_project_spring_boot.repository.SettingAuditLogRepository;
import com.example.demo_project_spring_boot.repository.SettingRepository;
import com.example.demo_project_spring_boot.service.CloudinaryService;
import com.example.demo_project_spring_boot.service.admin.SettingsManagementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsManagementServiceImpl implements SettingsManagementService {

    private static final String CACHE_NAME = "adminSettings";

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_SECURITY = "security";
    private static final String CATEGORY_EMAIL = "email";
    private static final String CATEGORY_PAYMENT = "payment";
    private static final String CATEGORY_NOTIFICATION = "notification";
    private static final String CATEGORY_SEO = "seo";
    private static final String CATEGORY_SOCIAL = "social";
    private static final String CATEGORY_THEME = "theme";

    private final SettingRepository settingRepository;
    private final SettingAuditLogRepository settingAuditLogRepository;
    private final SettingsMapper settingsMapper;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;
    private final SettingsUploadProperties settingsUploadProperties;

    @Value("${spring.application.name:demo_project_spring_boot}")
    private String applicationName;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'general:' + #tenant + ':' + #locale")
    public GeneralSettingsDto getGeneralSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_GENERAL, tenant, locale, GeneralSettingsDto.class, defaultsGeneral());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public GeneralSettingsDto updateGeneralSettings(GeneralSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_GENERAL, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_GENERAL, tenant, locale, GeneralSettingsDto.class, defaultsGeneral());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'security:' + #tenant + ':' + #locale")
    public SecuritySettingsDto getSecuritySettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_SECURITY, tenant, locale, SecuritySettingsDto.class, defaultsSecurity());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SecuritySettingsDto updateSecuritySettings(SecuritySettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_SECURITY, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_SECURITY, tenant, locale, SecuritySettingsDto.class, defaultsSecurity());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'email:' + #tenant + ':' + #locale")
    public EmailSettingsDto getEmailSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_EMAIL, tenant, locale, EmailSettingsDto.class, defaultsEmail());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public EmailSettingsDto updateEmailSettings(EmailSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_EMAIL, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_EMAIL, tenant, locale, EmailSettingsDto.class, defaultsEmail());
    }

    @Override
    public void sendTestEmail(TestEmailRequest request, String tenant, String locale) {
        EmailSettingsDto emailSettings = loadCategoryDto(CATEGORY_EMAIL, tenant, locale, EmailSettingsDto.class, defaultsEmail());
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(emailSettings.getSmtpHost());
        sender.setPort(emailSettings.getSmtpPort());
        sender.setUsername(emailSettings.getSmtpUsername());
        sender.setPassword(emailSettings.getSmtpPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(emailSettings.getTlsEnabled())));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailSettings.getSenderEmail());
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());
        sender.send(message);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'payment:' + #tenant + ':' + #locale")
    public PaymentSettingsDto getPaymentSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_PAYMENT, tenant, locale, PaymentSettingsDto.class, defaultsPayment());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public PaymentSettingsDto updatePaymentSettings(PaymentSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_PAYMENT, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_PAYMENT, tenant, locale, PaymentSettingsDto.class, defaultsPayment());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'notification:' + #tenant + ':' + #locale")
    public NotificationSettingsDto getNotificationSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_NOTIFICATION, tenant, locale, NotificationSettingsDto.class, defaultsNotification());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public NotificationSettingsDto updateNotificationSettings(NotificationSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_NOTIFICATION, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_NOTIFICATION, tenant, locale, NotificationSettingsDto.class, defaultsNotification());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'seo:' + #tenant + ':' + #locale")
    public SeoSettingsDto getSeoSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_SEO, tenant, locale, SeoSettingsDto.class, defaultsSeo());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SeoSettingsDto updateSeoSettings(SeoSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_SEO, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_SEO, tenant, locale, SeoSettingsDto.class, defaultsSeo());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'social:' + #tenant + ':' + #locale")
    public SocialSettingsDto getSocialSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_SOCIAL, tenant, locale, SocialSettingsDto.class, defaultsSocial());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public SocialSettingsDto updateSocialSettings(SocialSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_SOCIAL, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_SOCIAL, tenant, locale, SocialSettingsDto.class, defaultsSocial());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'theme:' + #tenant + ':' + #locale")
    public ThemeSettingsDto getThemeSettings(String tenant, String locale) {
        return loadCategoryDto(CATEGORY_THEME, tenant, locale, ThemeSettingsDto.class, defaultsTheme());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ThemeSettingsDto updateThemeSettings(ThemeSettingsDto request, String updatedBy, String tenant, String locale) {
        upsertCategory(CATEGORY_THEME, settingsMapper.toMap(request), updatedBy, tenant, locale, true);
        return loadCategoryDto(CATEGORY_THEME, tenant, locale, ThemeSettingsDto.class, defaultsTheme());
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public UploadAssetResponse uploadLogo(MultipartFile file, String updatedBy, String tenant, String locale) {
        validateAssetUpload(file, List.of("image/png", "image/jpeg", "image/svg+xml"));
        return uploadThemeAsset(file, "logoUrl", "logoPublicId", "logo", updatedBy, tenant, locale);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public UploadAssetResponse uploadFavicon(MultipartFile file, String updatedBy, String tenant, String locale) {
        validateAssetUpload(file, List.of("image/png", "image/x-icon", "image/vnd.microsoft.icon", "image/svg+xml"));
        return uploadThemeAsset(file, "faviconUrl", "faviconPublicId", "favicon", updatedBy, tenant, locale);
    }

    @Override
    public Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        return Map.of(
                "application", applicationName,
                "javaVersion", System.getProperty("java.version"),
                "availableProcessors", runtime.availableProcessors(),
                "maxMemory", runtime.maxMemory(),
                "totalMemory", runtime.totalMemory(),
                "freeMemory", runtime.freeMemory(),
                "uptimeMs", uptimeMs,
                "timestamp", Instant.now().toString()
        );
    }

    @Override
    public Map<String, Object> getHealth() {
        boolean databaseUp;
        try {
            settingRepository.count();
            databaseUp = true;
        } catch (Exception ex) {
            log.error("Health check failed for settings database", ex);
            databaseUp = false;
        }

        return Map.of(
                "status", databaseUp ? "UP" : "DEGRADED",
                "database", databaseUp ? "UP" : "DOWN",
                "cache", "UP",
                "timestamp", Instant.now().toString()
        );
    }

    @Override
    public SettingsBackupResponse backupSettings(String tenant, String locale) {
        String safeTenant = normalizeTenant(tenant);
        String safeLocale = normalizeLocale(locale);

        Map<String, Map<String, Object>> categories = new LinkedHashMap<>();
        categories.put(CATEGORY_GENERAL, settingsMapper.toMap(loadCategoryDto(CATEGORY_GENERAL, safeTenant, safeLocale, GeneralSettingsDto.class, defaultsGeneral())));
        categories.put(CATEGORY_SECURITY, settingsMapper.toMap(loadCategoryDto(CATEGORY_SECURITY, safeTenant, safeLocale, SecuritySettingsDto.class, defaultsSecurity())));
        categories.put(CATEGORY_EMAIL, settingsMapper.toMap(loadCategoryDto(CATEGORY_EMAIL, safeTenant, safeLocale, EmailSettingsDto.class, defaultsEmail())));
        categories.put(CATEGORY_PAYMENT, settingsMapper.toMap(loadCategoryDto(CATEGORY_PAYMENT, safeTenant, safeLocale, PaymentSettingsDto.class, defaultsPayment())));
        categories.put(CATEGORY_NOTIFICATION, settingsMapper.toMap(loadCategoryDto(CATEGORY_NOTIFICATION, safeTenant, safeLocale, NotificationSettingsDto.class, defaultsNotification())));
        categories.put(CATEGORY_SEO, settingsMapper.toMap(loadCategoryDto(CATEGORY_SEO, safeTenant, safeLocale, SeoSettingsDto.class, defaultsSeo())));
        categories.put(CATEGORY_SOCIAL, settingsMapper.toMap(loadCategoryDto(CATEGORY_SOCIAL, safeTenant, safeLocale, SocialSettingsDto.class, defaultsSocial())));
        categories.put(CATEGORY_THEME, settingsMapper.toMap(loadCategoryDto(CATEGORY_THEME, safeTenant, safeLocale, ThemeSettingsDto.class, defaultsTheme())));

        return SettingsBackupResponse.builder()
                .tenant(safeTenant)
                .locale(safeLocale)
                .exportedAt(Instant.now())
                .categories(categories)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void restoreSettings(SettingsRestoreRequest request, String updatedBy, String tenant, String locale) {
        String safeTenant = normalizeTenant(tenant);
        String safeLocale = normalizeLocale(locale);

        for (Map.Entry<String, Map<String, Object>> categoryEntry : request.getCategories().entrySet()) {
            if (categoryEntry.getValue() == null) {
                continue;
            }
            upsertCategory(categoryEntry.getKey(), categoryEntry.getValue(), updatedBy, safeTenant, safeLocale, request.isOverwriteExisting());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void importSettingsJson(MultipartFile file, String updatedBy, String tenant, String locale) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Settings import file is required");
        }

        try {
            Map<String, Object> root = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {
            });
            Object categoriesObject = root.getOrDefault("categories", root);
            Map<String, Map<String, Object>> categories = objectMapper.convertValue(categoriesObject, new TypeReference<>() {
            });

            SettingsRestoreRequest restoreRequest = new SettingsRestoreRequest();
            restoreRequest.setCategories(categories);
            restoreRequest.setOverwriteExisting(true);
            restoreSettings(restoreRequest, updatedBy, tenant, locale);
        } catch (IOException ex) {
            throw new BadRequestException("Invalid settings JSON file");
        }
    }

    private <T> T loadCategoryDto(String category,
                                  String tenant,
                                  String locale,
                                  Class<T> dtoClass,
                                  Map<String, Object> defaults) {
        String safeTenant = normalizeTenant(tenant);
        String safeLocale = normalizeLocale(locale);

        Map<String, Object> values = new LinkedHashMap<>(defaults);
        List<Setting> settings = settingRepository.findByCategoryAndTenantKeyAndLocaleKey(category, safeTenant, safeLocale);

        for (Setting setting : settings) {
            values.put(extractFieldName(setting.getSettingKey(), category), fromJson(setting.getSettingValue()));
        }

        return settingsMapper.toDto(values, dtoClass);
    }

    private void upsertCategory(String category,
                                Map<String, Object> values,
                                String updatedBy,
                                String tenant,
                                String locale,
                                boolean overwriteExisting) {
        String safeTenant = normalizeTenant(tenant);
        String safeLocale = normalizeLocale(locale);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String field = entry.getKey();
            String key = category + "." + field;

            Optional<Setting> existing = settingRepository.findBySettingKeyAndTenantKeyAndLocaleKey(key, safeTenant, safeLocale);
            if (existing.isPresent() && !overwriteExisting) {
                continue;
            }

            String newValue = toJson(entry.getValue());
            String oldValue = existing.map(Setting::getSettingValue).orElse(null);

            Setting setting = existing.orElseGet(() -> Setting.builder()
                    .settingKey(key)
                    .category(category)
                    .tenantKey(safeTenant)
                    .localeKey(safeLocale)
                    .description("Managed by admin settings")
                    .build());

            setting.setSettingValue(newValue);
            setting.setUpdatedBy(updatedBy);
            settingRepository.save(setting);

            settingAuditLogRepository.save(SettingAuditLog.builder()
                    .settingKey(key)
                    .actionType(existing.isPresent() ? "UPDATE" : "CREATE")
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .updatedBy(updatedBy)
                    .tenantKey(safeTenant)
                    .localeKey(safeLocale)
                    .build());
        }
    }

    private UploadAssetResponse uploadThemeAsset(MultipartFile file,
                                                 String urlField,
                                                 String publicIdField,
                                                 String assetType,
                                                 String updatedBy,
                                                 String tenant,
                                                 String locale) {
        try {
            Map<?, ?> upload = cloudinaryService.uploadImage(file, settingsUploadProperties.getCloudinaryFolder());
            String secureUrl = Objects.toString(upload.get("secure_url"), "");
            String publicId = Objects.toString(upload.get("public_id"), "");

            Map<String, Object> assetMap = new HashMap<>();
            assetMap.put(urlField, secureUrl);
            assetMap.put(publicIdField, publicId);
            upsertCategory(CATEGORY_THEME, assetMap, updatedBy, tenant, locale, true);

            log.info("Admin settings asset uploaded. type={}, user={}", assetType, updatedBy);
            return UploadAssetResponse.builder()
                    .assetType(assetType)
                    .url(secureUrl)
                    .publicId(publicId)
                    .build();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to upload " + assetType + " file");
        }
    }

    private void validateAssetUpload(MultipartFile file, List<String> allowedForEndpoint) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > settingsUploadProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException("File exceeds maximum upload size");
        }
        String contentType = file.getContentType();
        List<String> globalAllowed = settingsUploadProperties.getAllowedContentTypes();
        if (contentType == null || !globalAllowed.contains(contentType) || !allowedForEndpoint.contains(contentType)) {
            throw new BadRequestException("Unsupported file type");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new BadRequestException("Could not serialize setting value");
        }
    }

    private Object fromJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (IOException ex) {
            return value;
        }
    }

    private String extractFieldName(String settingKey, String category) {
        String prefix = category + ".";
        return settingKey.startsWith(prefix) ? settingKey.substring(prefix.length()) : settingKey;
    }

    private String normalizeTenant(String tenant) {
        return tenant == null || tenant.isBlank() ? "default" : tenant.trim();
    }

    private String normalizeLocale(String locale) {
        return locale == null || locale.isBlank() ? "en" : locale.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> defaultsGeneral() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("websiteName", "TableEco");
        defaults.put("websiteDescription", "E-commerce administration");
        defaults.put("contactEmail", "support@example.com");
        defaults.put("contactPhone", "+85500000000");
        defaults.put("address", "Phnom Penh");
        defaults.put("companyInformation", "");
        defaults.put("timezone", "Asia/Phnom_Penh");
        defaults.put("currency", "USD");
        defaults.put("language", "en");
        defaults.put("dateFormat", "yyyy-MM-dd");
        return defaults;
    }

    private Map<String, Object> defaultsSecurity() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("jwtExpirationSeconds", 86400);
        defaults.put("passwordMinLength", 8);
        defaults.put("requireUppercase", true);
        defaults.put("requireLowercase", true);
        defaults.put("requireNumber", true);
        defaults.put("requireSpecialCharacter", false);
        defaults.put("loginAttemptLimit", 5);
        defaults.put("sessionTimeoutMinutes", 60);
        defaults.put("twoFactorEnabled", false);
        defaults.put("corsEnabled", true);
        defaults.put("allowedOrigins", List.of("http://localhost:3000", "http://localhost:3001"));
        defaults.put("apiRateLimitPerMinute", 120);
        return defaults;
    }

    private Map<String, Object> defaultsEmail() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("smtpHost", "smtp.gmail.com");
        defaults.put("smtpPort", 587);
        defaults.put("smtpUsername", "");
        defaults.put("smtpPassword", "");
        defaults.put("senderEmail", "noreply@example.com");
        defaults.put("senderName", "TableEco");
        defaults.put("emailVerificationEnabled", true);
        defaults.put("forgotPasswordTemplate", "Hello {{name}}, click {{link}} to reset your password.");
        defaults.put("tlsEnabled", true);
        return defaults;
    }

    private Map<String, Object> defaultsPayment() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("bakongEnabled", false);
        defaults.put("bakongMerchantId", "");
        defaults.put("abaEnabled", false);
        defaults.put("abaMerchantId", "");
        defaults.put("stripeEnabled", false);
        defaults.put("stripePublicKey", "");
        defaults.put("stripeSecretKey", "");
        defaults.put("paypalEnabled", false);
        defaults.put("paypalClientId", "");
        defaults.put("paypalSecret", "");
        defaults.put("baseCurrency", "USD");
        defaults.put("transactionFeePercent", BigDecimal.ZERO);
        defaults.put("flatTransactionFee", BigDecimal.ZERO);
        defaults.put("exchangeRateUsd", BigDecimal.ONE);
        return defaults;
    }

    private Map<String, Object> defaultsNotification() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("emailNotificationsEnabled", true);
        defaults.put("pushNotificationsEnabled", true);
        defaults.put("smsNotificationsEnabled", false);
        defaults.put("adminAlertsEnabled", true);
        defaults.put("userOrderNotificationsEnabled", true);
        defaults.put("userPromotionNotificationsEnabled", true);
        defaults.put("userSecurityNotificationsEnabled", true);
        return defaults;
    }

    private Map<String, Object> defaultsSeo() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("metaTitle", "TableEco Ecommerce");
        defaults.put("metaDescription", "Modern ecommerce experience");
        defaults.put("metaKeywords", "ecommerce, tableeco");
        defaults.put("openGraphTitle", "TableEco");
        defaults.put("openGraphDescription", "Modern ecommerce experience");
        defaults.put("openGraphImageUrl", "");
        defaults.put("googleAnalyticsId", "");
        defaults.put("facebookPixelId", "");
        return defaults;
    }

    private Map<String, Object> defaultsSocial() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("facebookUrl", "");
        defaults.put("telegramUrl", "");
        defaults.put("instagramUrl", "");
        defaults.put("tiktokUrl", "");
        defaults.put("youtubeUrl", "");
        defaults.put("linkedInUrl", "");
        return defaults;
    }

    private Map<String, Object> defaultsTheme() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("primaryColor", "#1677ff");
        defaults.put("secondaryColor", "#13c2c2");
        defaults.put("mode", "light");
        defaults.put("sidebarStyle", "compact");
        defaults.put("dashboardLayout", "default");
        defaults.put("logoUrl", "");
        defaults.put("faviconUrl", "");
        return defaults;
    }
}


