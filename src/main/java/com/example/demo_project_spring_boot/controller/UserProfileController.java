package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ApiResult;
import com.example.demo_project_spring_boot.dto.UpdateProfileWithTokenResponse;
import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.dto.request.ChangeUserPasswordRequest;
import com.example.demo_project_spring_boot.dto.request.NotificationSettingsRequest;
import com.example.demo_project_spring_boot.dto.request.UpdateUserProfileRequest;
import com.example.demo_project_spring_boot.dto.request.UserAddressRequest;
import com.example.demo_project_spring_boot.dto.response.UserAddressResponse;
import com.example.demo_project_spring_boot.service.UserProfileService;
import com.example.demo_project_spring_boot.service.impl.UserProfileServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/users"})
@RequiredArgsConstructor
@Tag(name = "User Profile APIs", description = "Profile APIs for authenticated users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(summary = "Get User Profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> getProfile(Authentication authentication) {
        UserProfileResponse payload = userProfileService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResult.<UserProfileResponse>builder()
                .success(true)
                .message("Profile fetched successfully")
                .data(payload)
                .build());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile with token regeneration if username/email changes", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UpdateProfileWithTokenResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        // Cast to implementation to access the new method with token regeneration
        UserProfileServiceImpl serviceImpl = (UserProfileServiceImpl) userProfileService;
        UpdateProfileWithTokenResponse payload = serviceImpl.updateMyProfileWithTokens(
                authentication.getName(), 
                request
        );
        return ResponseEntity.ok(ApiResult.<UpdateProfileWithTokenResponse>builder()
                .success(true)
                .message(payload.getMessage())
                .data(payload)
                .build());
    }

    @PutMapping("/profile/change-password")
    @Operation(summary = "Change user password", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangeUserPasswordRequest request
    ) {
        userProfileService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResult.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }

    @PutMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace profile image", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UploadImageResponse>> uploadProfileImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        UploadImageResponse payload = userProfileService.uploadProfileImage(authentication.getName(), file);
        return ResponseEntity.ok(ApiResult.<UploadImageResponse>builder()
                .success(true)
                .message("Profile image uploaded successfully")
                .data(payload)
                .build());
    }

    @DeleteMapping("/profile/image")
    @Operation(summary = "Delete profile image", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> deleteProfileImage(Authentication authentication) {
        UserProfileResponse payload = userProfileService.deleteProfileImage(authentication.getName());
        return ResponseEntity.ok(ApiResult.<UserProfileResponse>builder()
                .success(true)
                .message("Profile image deleted successfully")
                .data(payload)
                .build());
    }

    @GetMapping("/profile/addresses")
    @Operation(summary = "Get profile addresses", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<UserAddressResponse>>> getAddresses(Authentication authentication) {
        List<UserAddressResponse> payload = userProfileService.getMyAddresses(authentication.getName());
        return ResponseEntity.ok(ApiResult.<List<UserAddressResponse>>builder()
                .success(true)
                .message("Addresses fetched successfully")
                .data(payload)
                .build());
    }

    @PostMapping("/profile/addresses")
    @Operation(summary = "Add profile address", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserAddressResponse>> addAddress(
            Authentication authentication,
            @Valid @RequestBody UserAddressRequest request
    ) {
        UserAddressResponse payload = userProfileService.addAddress(authentication.getName(), request);
        return ResponseEntity.ok(ApiResult.<UserAddressResponse>builder()
                .success(true)
                .message("Address added successfully")
                .data(payload)
                .build());
    }

    @PutMapping("/profile/addresses/{id}")
    @Operation(summary = "Update profile address", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserAddressResponse>> updateAddress(
            Authentication authentication,
            @PathVariable("id") Long addressId,
            @Valid @RequestBody UserAddressRequest request
    ) {
        UserAddressResponse payload = userProfileService.updateAddress(authentication.getName(), addressId, request);
        return ResponseEntity.ok(ApiResult.<UserAddressResponse>builder()
                .success(true)
                .message("Address updated successfully")
                .data(payload)
                .build());
    }

    @DeleteMapping("/profile/addresses/{id}")
    @Operation(summary = "Delete profile address", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> deleteAddress(
            Authentication authentication,
            @PathVariable("id") Long addressId
    ) {
        userProfileService.deleteAddress(authentication.getName(), addressId);
        return ResponseEntity.ok(ApiResult.<Void>builder()
                .success(true)
                .message("Address deleted successfully")
                .build());
    }

    @PutMapping("/profile/settings")
    @Operation(summary = "Update profile notification settings", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateSettings(
            Authentication authentication,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        UserProfileResponse payload = userProfileService.updateNotificationSettings(authentication.getName(), request);
        return ResponseEntity.ok(ApiResult.<UserProfileResponse>builder()
                .success(true)
                .message("Notification settings updated successfully")
                .data(payload)
                .build());
    }
}
