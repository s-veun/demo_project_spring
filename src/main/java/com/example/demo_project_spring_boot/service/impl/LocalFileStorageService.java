package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.exception.FileStorageException;
import com.example.demo_project_spring_boot.service.FileStorageService;
import com.example.demo_project_spring_boot.service.StoredImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.profile-image.storage-provider", havingValue = "local")
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final ProfileImageProperties properties;

    @Override
    public StoredImage storeProfileImage(MultipartFile file, String username, String extension) {
        Path uploadRoot = resolveUploadRoot();
        String imageName = buildSafeFileName(username, extension);
        Path destination = uploadRoot.resolve(imageName).normalize();

        if (!destination.startsWith(uploadRoot)) {
            throw new FileStorageException("Invalid upload path");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            String imageUrl = buildPublicUrl(imageName);
            log.info("Stored profile image. user={}, file={}", username, imageName);
            return new StoredImage(imageName, imageUrl);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store profile image", ex);
        }
    }

    @Override
    public void deleteProfileImage(String imageName) {
        if (!StringUtils.hasText(imageName)) {
            return;
        }

        Path uploadRoot = resolveUploadRoot();
        Path target = uploadRoot.resolve(imageName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new FileStorageException("Invalid file path");
        }

        try {
            Files.deleteIfExists(target);
            log.info("Deleted profile image file={}", imageName);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete profile image", ex);
        }
    }

    private Path resolveUploadRoot() {
        try {
            Path uploadRoot = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
            return uploadRoot;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to prepare upload directory", ex);
        }
    }

    private String buildSafeFileName(String username, String extension) {
        String safeUsername = username.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
        if (safeUsername.isBlank()) {
            safeUsername = "user";
        }
        return safeUsername + "-" + UUID.randomUUID() + "." + extension;
    }

    private String buildPublicUrl(String imageName) {
        String basePath = trimTrailingSlash(properties.getPublicBasePath());
        String apiBaseUrl = trimTrailingSlash(properties.getApiBaseUrl());

        if (StringUtils.hasText(apiBaseUrl)) {
            return apiBaseUrl + basePath + "/" + imageName;
        }
        return basePath + "/" + imageName;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

