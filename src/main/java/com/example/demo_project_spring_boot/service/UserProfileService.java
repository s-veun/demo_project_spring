package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.dto.request.ChangeUserPasswordRequest;
import com.example.demo_project_spring_boot.dto.request.NotificationSettingsRequest;
import com.example.demo_project_spring_boot.dto.request.UpdateUserProfileRequest;
import com.example.demo_project_spring_boot.dto.request.UserAddressRequest;
import com.example.demo_project_spring_boot.dto.response.UserAddressResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserProfileService {
    UserProfileResponse getMyProfile(String principalName);
    UserProfileResponse updateMyProfile(String principalName, UpdateUserProfileRequest request);
    void changePassword(String principalName, ChangeUserPasswordRequest request);
    UploadImageResponse uploadProfileImage(String principalName, MultipartFile file);
    UserProfileResponse deleteProfileImage(String principalName);
    List<UserAddressResponse> getMyAddresses(String principalName);
    UserAddressResponse addAddress(String principalName, UserAddressRequest request);
    UserAddressResponse updateAddress(String principalName, Long addressId, UserAddressRequest request);
    void deleteAddress(String principalName, Long addressId);
    UserProfileResponse updateNotificationSettings(String principalName, NotificationSettingsRequest request);
}

