package com.example.demo_project_spring_boot.controller.admin;

import com.example.demo_project_spring_boot.dto.ApiResponse;
import com.example.demo_project_spring_boot.dto.admin.settings.*;
import com.example.demo_project_spring_boot.service.admin.SettingsManagementService;
import com.example.demo_project_spring_boot.utils.ApiResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Settings", description = "System settings management APIs")
public class AdminSettingsController {

    private final SettingsManagementService settingsManagementService;

    @GetMapping("/general")
    @Operation(summary = "Get general settings")
    public ResponseEntity<ApiResponse<GeneralSettingsDto>> getGeneral(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getGeneralSettings(tenant, locale)));
    }

    @PutMapping("/general")
    @Operation(summary = "Update general settings")
    public ResponseEntity<ApiResponse<GeneralSettingsDto>> updateGeneral(
            @RequestBody @Valid GeneralSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateGeneralSettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/security")
    @Operation(summary = "Get security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsDto>> getSecurity(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getSecuritySettings(tenant, locale)));
    }

    @PutMapping("/security")
    @Operation(summary = "Update security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsDto>> updateSecurity(
            @RequestBody @Valid SecuritySettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateSecuritySettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/email")
    @Operation(summary = "Get email settings")
    public ResponseEntity<ApiResponse<EmailSettingsDto>> getEmail(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getEmailSettings(tenant, locale)));
    }

    @PutMapping("/email")
    @Operation(summary = "Update email settings")
    public ResponseEntity<ApiResponse<EmailSettingsDto>> updateEmail(
            @RequestBody @Valid EmailSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateEmailSettings(request, authentication.getName(), tenant, locale)));
    }

    @PostMapping("/email/test")
    @Operation(summary = "Send test email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testEmail(
            @RequestBody @Valid TestEmailRequest request,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        settingsManagementService.sendTestEmail(request, tenant, locale);
        return ResponseEntity.ok(ApiResponseUtils.success("Test email sent successfully", Map.of("to", request.getTo())));
    }

    @GetMapping("/payment")
    @Operation(summary = "Get payment settings")
    public ResponseEntity<ApiResponse<PaymentSettingsDto>> getPayment(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getPaymentSettings(tenant, locale)));
    }

    @PutMapping("/payment")
    @Operation(summary = "Update payment settings")
    public ResponseEntity<ApiResponse<PaymentSettingsDto>> updatePayment(
            @RequestBody @Valid PaymentSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updatePaymentSettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/notification")
    @Operation(summary = "Get notification settings")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> getNotification(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getNotificationSettings(tenant, locale)));
    }

    @PutMapping("/notification")
    @Operation(summary = "Update notification settings")
    public ResponseEntity<ApiResponse<NotificationSettingsDto>> updateNotification(
            @RequestBody @Valid NotificationSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateNotificationSettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/seo")
    @Operation(summary = "Get SEO settings")
    public ResponseEntity<ApiResponse<SeoSettingsDto>> getSeo(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getSeoSettings(tenant, locale)));
    }

    @PutMapping("/seo")
    @Operation(summary = "Update SEO settings")
    public ResponseEntity<ApiResponse<SeoSettingsDto>> updateSeo(
            @RequestBody @Valid SeoSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateSeoSettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/social")
    @Operation(summary = "Get social media settings")
    public ResponseEntity<ApiResponse<SocialSettingsDto>> getSocial(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getSocialSettings(tenant, locale)));
    }

    @PutMapping("/social")
    @Operation(summary = "Update social media settings")
    public ResponseEntity<ApiResponse<SocialSettingsDto>> updateSocial(
            @RequestBody @Valid SocialSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateSocialSettings(request, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/theme")
    @Operation(summary = "Get theme settings")
    public ResponseEntity<ApiResponse<ThemeSettingsDto>> getTheme(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getThemeSettings(tenant, locale)));
    }

    @PutMapping("/theme")
    @Operation(summary = "Update theme settings")
    public ResponseEntity<ApiResponse<ThemeSettingsDto>> updateTheme(
            @RequestBody @Valid ThemeSettingsDto request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Settings updated successfully",
                settingsManagementService.updateThemeSettings(request, authentication.getName(), tenant, locale)));
    }

    @PostMapping(value = "/upload/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload website logo", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = MultipartFile.class))))
    public ResponseEntity<ApiResponse<UploadAssetResponse>> uploadLogo(
            @RequestPart("file") MultipartFile file,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Logo uploaded successfully",
                settingsManagementService.uploadLogo(file, authentication.getName(), tenant, locale)));
    }

    @PostMapping(value = "/upload/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload website favicon", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = MultipartFile.class))))
    public ResponseEntity<ApiResponse<UploadAssetResponse>> uploadFavicon(
            @RequestPart("file") MultipartFile file,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success("Favicon uploaded successfully",
                settingsManagementService.uploadFavicon(file, authentication.getName(), tenant, locale)));
    }

    @GetMapping("/system-info")
    @Operation(summary = "Get system runtime info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> systemInfo() {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getSystemInfo()));
    }

    @GetMapping("/health")
    @Operation(summary = "Get settings service health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.getHealth()));
    }

    @GetMapping("/backup")
    @Operation(summary = "Backup settings as JSON-ready payload")
    public ResponseEntity<ApiResponse<SettingsBackupResponse>> backup(
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        return ResponseEntity.ok(ApiResponseUtils.success(settingsManagementService.backupSettings(tenant, locale)));
    }

    @PostMapping("/restore")
    @Operation(summary = "Restore settings from backup payload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restore(
            @RequestBody @Valid SettingsRestoreRequest request,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        settingsManagementService.restoreSettings(request, authentication.getName(), tenant, locale);
        return ResponseEntity.ok(ApiResponseUtils.success("Settings restored successfully", Map.of("restored", true)));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import settings from JSON file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importJson(
            @RequestPart("file") MultipartFile file,
            Authentication authentication,
            @RequestParam(defaultValue = "default") String tenant,
            @RequestParam(defaultValue = "en") String locale
    ) {
        settingsManagementService.importSettingsJson(file, authentication.getName(), tenant, locale);
        return ResponseEntity.ok(ApiResponseUtils.success("Settings imported successfully", Map.of("imported", true)));
    }
}

