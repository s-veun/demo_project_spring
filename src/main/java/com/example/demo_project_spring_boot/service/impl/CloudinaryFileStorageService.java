package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import com.example.demo_project_spring_boot.exception.FileStorageException;
import com.example.demo_project_spring_boot.service.CloudinaryService;
import com.example.demo_project_spring_boot.service.FileStorageService;
import com.example.demo_project_spring_boot.service.StoredImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.profile-image.storage-provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryFileStorageService implements FileStorageService {

    private final CloudinaryService cloudinaryService;
    private final ProfileImageProperties profileImageProperties;

    @Override
    public StoredImage storeProfileImage(MultipartFile file, String username, String extension) {
        try {
            Map<?, ?> uploadResult = cloudinaryService.uploadImage(file, profileImageProperties.getCloudinaryFolder());
            String publicId = String.valueOf(uploadResult.get("public_id"));
            String secureUrl = String.valueOf(uploadResult.get("secure_url"));
            log.info("Stored profile image on Cloudinary. user={}, publicId={}", username, publicId);
            return new StoredImage(publicId, secureUrl);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to upload profile image to Cloudinary", ex);
        }
    }

    @Override
    public void deleteProfileImage(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return;
        }

        try {
            cloudinaryService.deleteFile(imageName);
            log.info("Deleted Cloudinary profile image. publicId={}", imageName);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete profile image from Cloudinary", ex);
        }
    }
}


