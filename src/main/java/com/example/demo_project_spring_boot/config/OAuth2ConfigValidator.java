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

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:{baseUrl}/login/oauth2/code/{registrationId}}")
    private String googleRedirectUriTemplate;

    @Value("${app.frontend-url:}")
    private String frontendRedirectUri;

    @Value("${SPRING_PROFILES_ACTIVE:}")
    private String activeProfiles;

    @PostConstruct
    public void validate() {
        boolean hasGoogleId = StringUtils.hasText(googleClientId);
        boolean hasGoogleSecret = StringUtils.hasText(googleClientSecret);
        boolean hasGoogleRedirect = StringUtils.hasText(googleRedirectUriTemplate);

        if (!hasGoogleId || !hasGoogleSecret || !hasGoogleRedirect) {
            log.warn("OAuth2 Google configuration is incomplete. Google login will fail with invalid_client.");
            log.warn("Missing values => clientId:{}, clientSecret:{}, redirectUri:{}",
                    hasGoogleId, hasGoogleSecret, hasGoogleRedirect);
            return;
        }

        if (looksLikePlaceholder(googleClientId) || looksLikePlaceholder(googleClientSecret)) {
            throw new IllegalStateException("Google OAuth2 credentials are placeholders. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET correctly.");
        }

        log.info("Google OAuth2 client is configured.");
        log.info("Google redirect URI template: {}", googleRedirectUriTemplate);
        log.info("Make sure Google Console redirect URI EXACTLY matches: https://<your-backend-domain>/login/oauth2/code/google");

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

