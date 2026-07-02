package com.example.demo_project_spring_boot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profile Update Response with JWT Tokens
 * Returned when profile is updated, including new tokens if username/email changed
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProfileWithTokenResponse {
    private boolean success;
    private String message;
    
    // Profile data
    private UserProfileResponse profile;
    
    // New tokens (only if username/email changed)
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // in seconds
    
    // Flag indicating if tokens were regenerated
    private boolean tokensRegenerated;
    
    // Which fields were updated that required token regeneration
    private UpdatedSecurityField securityFieldUpdated;
    
    public enum UpdatedSecurityField {
        NONE,
        USERNAME,
        EMAIL,
        BOTH
    }
}
