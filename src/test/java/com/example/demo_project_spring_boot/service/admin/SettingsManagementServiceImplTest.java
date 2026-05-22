package com.example.demo_project_spring_boot.service.admin;

import com.example.demo_project_spring_boot.config.SettingsUploadProperties;
import com.example.demo_project_spring_boot.dto.admin.settings.GeneralSettingsDto;
import com.example.demo_project_spring_boot.dto.admin.settings.SettingsRestoreRequest;
import com.example.demo_project_spring_boot.mapper.SettingsMapper;
import com.example.demo_project_spring_boot.model.Setting;
import com.example.demo_project_spring_boot.repository.SettingAuditLogRepository;
import com.example.demo_project_spring_boot.repository.SettingRepository;
import com.example.demo_project_spring_boot.service.CloudinaryService;
import com.example.demo_project_spring_boot.service.admin.impl.SettingsManagementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsManagementServiceImplTest {

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private SettingAuditLogRepository settingAuditLogRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    private SettingsManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        SettingsMapper settingsMapper = new SettingsMapper(objectMapper);
        SettingsUploadProperties properties = new SettingsUploadProperties();
        service = new SettingsManagementServiceImpl(
                settingRepository,
                settingAuditLogRepository,
                settingsMapper,
                objectMapper,
                cloudinaryService,
                properties
        );
    }

    @Test
    void getGeneralSettingsReturnsDefaultsWhenNoData() {
        when(settingRepository.findByCategoryAndTenantKeyAndLocaleKey(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        GeneralSettingsDto dto = service.getGeneralSettings("default", "en");

        assertNotNull(dto);
        assertEquals("TableEco", dto.getWebsiteName());
        assertEquals("USD", dto.getCurrency());
    }

    @Test
    void updateGeneralSettingsPersistsRows() {
        when(settingRepository.findBySettingKeyAndTenantKeyAndLocaleKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(Setting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(settingRepository.findByCategoryAndTenantKeyAndLocaleKey(eq("general"), anyString(), anyString()))
                .thenAnswer(invocation -> List.of());

        GeneralSettingsDto request = new GeneralSettingsDto();
        request.setWebsiteName("My Shop");
        request.setWebsiteDescription("Description");
        request.setContactEmail("admin@example.com");
        request.setContactPhone("+85512345678");
        request.setAddress("Phnom Penh");
        request.setCompanyInformation("Company");
        request.setTimezone("Asia/Phnom_Penh");
        request.setCurrency("USD");
        request.setLanguage("en");
        request.setDateFormat("yyyy-MM-dd");

        service.updateGeneralSettings(request, "admin", "default", "en");

        verify(settingRepository, atLeastOnce()).save(any(Setting.class));
        verify(settingAuditLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void restoreSettingsPersistsCategories() {
        when(settingRepository.findBySettingKeyAndTenantKeyAndLocaleKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        SettingsRestoreRequest request = new SettingsRestoreRequest();
        request.setCategories(Map.of(
                "general", Map.of("websiteName", "Restored Shop"),
                "theme", Map.of("primaryColor", "#111111")
        ));

        service.restoreSettings(request, "admin", "default", "en");

        verify(settingRepository, atLeast(2)).save(any(Setting.class));
    }
}

