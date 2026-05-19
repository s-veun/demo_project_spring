package com.example.demo_project_spring_boot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates OAuth2 configuration at startup to avoid runtime "invalid_client" surprises.
 */
@Component
@Slf4j
public class OAuth2ConfigValidator {

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String googleClientSecret;

    private final String googleRedirectUriTemplate = "{baseUrl}/login/oauth2/code/{registrationId}";

    @Value("${FACEBOOK_APP_ID:}")
    private String facebookClientId;

    @Value("${FACEBOOK_APP_SECRET:}")
    private String facebookClientSecret;

    @Value("${app.frontend-url:}")
    private String frontendRedirectUri;

    @Value("${SPRING_PROFILES_ACTIVE:}")
    private String activeProfiles;

    @PostConstruct
    public void validate() {
        boolean hasGoogleId = StringUtils.hasText(googleClientId);
        boolean hasGoogleSecret = StringUtils.hasText(googleClientSecret);
        boolean hasGoogleRedirect = StringUtils.hasText(googleRedirectUriTemplate);

        if (!hasGoogleId && !hasGoogleSecret) {
            log.info("Google OAuth2 credentials are not set. Google login is disabled.");
        } else if (!hasGoogleId || !hasGoogleSecret || !hasGoogleRedirect) {
            log.warn("OAuth2 Google configuration is incomplete. Google login is disabled until all values are set.");
            log.warn("Configured values => clientId:{}, clientSecret:{}, redirectUri:{}",
                    hasGoogleId, hasGoogleSecret, hasGoogleRedirect);
        } else {
            if (looksLikePlaceholder(googleClientId) || looksLikePlaceholder(googleClientSecret)) {
                throw new IllegalStateException("Google OAuth2 credentials are placeholders. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET correctly.");
            }

            log.info("Google OAuth2 client is configured.");
            log.info("Google redirect URI template: {}", googleRedirectUriTemplate);
            log.info("Make sure Google Console redirect URI EXACTLY matches: https://<your-backend-domain>/login/oauth2/code/google");
        }

        boolean hasFacebookId = StringUtils.hasText(facebookClientId);
        boolean hasFacebookSecret = StringUtils.hasText(facebookClientSecret);
        if (hasFacebookId ^ hasFacebookSecret) {
            log.warn("OAuth2 Facebook configuration is incomplete. Set both FACEBOOK_APP_ID and FACEBOOK_APP_SECRET.");
        } else if (hasFacebookId) {
            log.info("Facebook OAuth2 client is configured.");
            log.info("Make sure Facebook redirect URI EXACTLY matches: https://<your-backend-domain>/login/oauth2/code/facebook");
        }

        if (StringUtils.hasText(frontendRedirectUri)) {
            log.info("Frontend base URL: {}", frontendRedirectUri);
        }

        if (StringUtils.hasText(activeProfiles)) {
            log.info("Active Spring profiles: {}", activeProfiles);
        }
    }

    private boolean looksLikePlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.contains("replace_with") || normalized.contains("your_") || normalized.contains("placeholder");
    }
}

