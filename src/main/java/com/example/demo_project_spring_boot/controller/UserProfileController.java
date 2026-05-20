package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ApiResult;
import com.example.demo_project_spring_boot.dto.UploadImageResponse;
import com.example.demo_project_spring_boot.dto.UserProfileResponse;
import com.example.demo_project_spring_boot.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/user", "/api/v1/users"})
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
}
