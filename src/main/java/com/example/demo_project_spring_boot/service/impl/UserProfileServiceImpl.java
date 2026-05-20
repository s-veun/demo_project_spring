package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.exception.ResourceNotFoundException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.FileStorageService;
import com.example.demo_project_spring_boot.service.StoredImage;
import com.example.demo_project_spring_boot.service.UserProfileService;
import com.example.demo_project_spring_boot.utils.ProfileImageValidator;
import com.example.demo_project_spring_boot.config.ProfileImageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProfileImageValidator profileImageValidator;
    private final ProfileImageProperties profileImageProperties;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String principalName) {
        User user = findUserByPrincipal(principalName);
        return mapToProfileResponse(user);
    }

    @Override
    public UploadImageResponse uploadProfileImage(String principalName, MultipartFile file) {
        User user = findUserByPrincipal(principalName);
        String extension = profileImageValidator.validateAndExtractExtension(file);

        String oldImageName = user.getProfileImageName();
        if (StringUtils.hasText(oldImageName)) {
            fileStorageService.deleteProfileImage(oldImageName);
        }

        StoredImage storedImage = fileStorageService.storeProfileImage(file, user.getUsername(), extension);
        user.setProfileImageName(storedImage.imageName());
        user.setProfileImageUrl(storedImage.imageUrl());
        userRepository.save(user);

        log.info("Profile image updated for user={}", user.getUsername());

        return UploadImageResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .imageUrl(storedImage.imageUrl())
                .uploadTimestamp(OffsetDateTime.now())
                .build();
    }

    @Override
    public UserProfileResponse deleteProfileImage(String principalName) {
        User user = findUserByPrincipal(principalName);

        String oldImageName = user.getProfileImageName();
        if (StringUtils.hasText(oldImageName)) {
            fileStorageService.deleteProfileImage(oldImageName);
        }

        user.setProfileImageName(null);
        user.setProfileImageUrl(null);
        User saved = userRepository.save(user);
        log.info("Profile image removed for user={}", saved.getUsername());

        return mapToProfileResponse(saved);
    }

    private User findUserByPrincipal(String principalName) {
        return userRepository.findByUsername(principalName)
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principalName));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        String resolvedImageUrl = StringUtils.hasText(user.getProfileImageUrl())
                ? user.getProfileImageUrl()
                : profileImageProperties.getDefaultUrl();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(resolvedImageUrl)
                .role(user.getRole())
                .isEnabled(user.getIsEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

