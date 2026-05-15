package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.ApiResult;
import com.example.demo_project_spring_boot.dto.SocialUserProfileResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Social Authentication APIs", description = "Profile endpoint for authenticated social/local users")
public class UserProfileController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get User Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResult<SocialUserProfileResponse>> getProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SocialUserProfileResponse payload = SocialUserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImage(user.getProfileImageUrl())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResult.<SocialUserProfileResponse>builder()
                .success(true)
                .message("Profile fetched successfully")
                .data(payload)
                .build());
    }
}

