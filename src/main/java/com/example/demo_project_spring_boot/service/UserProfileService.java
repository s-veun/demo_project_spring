package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {
    UserProfileResponse getMyProfile(String principalName);
    UploadImageResponse uploadProfileImage(String principalName, MultipartFile file);
    UserProfileResponse deleteProfileImage(String principalName);
}

