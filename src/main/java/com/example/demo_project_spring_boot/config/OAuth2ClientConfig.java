package com.example.demo_project_spring_boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditional OAuth2 client registration.
 * <p>
 * The application only enables Google login when the Google credentials are present.
 * If the credentials are missing, the app still starts and social-login endpoints remain disabled.
 */
@Configuration
@Slf4j
public class OAuth2ClientConfig {

    // -----------------------------------------------------------------------
    // Google credentials  (env: GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)
    // -----------------------------------------------------------------------
    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth2.google.client-secret:}")
    private String googleClientSecret;

    // -----------------------------------------------------------------------
    // ClientRegistrationRepository — only includes providers with credentials
    // -----------------------------------------------------------------------

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret)) {
            registrations.add(buildGoogleRegistration());
            log.info("✅ Google OAuth2 client registered (clientId={}...)", googleClientId.substring(0, Math.min(8, googleClientId.length())));
        } else {
            log.warn("⚠️  GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET not set — Google login disabled.");
        }

        if (registrations.isEmpty()) {
            log.warn("⚠️  No OAuth2 providers configured. Social login is fully disabled.");
            // Return a no-op repo so SecurityConfig can still call oauth2Login() safely.
            // At runtime, any attempt to initiate OAuth2 flow will receive a 401.
            return registrationId -> null;
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    /** True when at least one provider has credentials; used by SecurityConfig. */
    public boolean hasAnyRegistration() {
        return StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret);
    }

    // -----------------------------------------------------------------------
    // Supporting OAuth2 beans (back-off when auto-config is excluded)
    // -----------------------------------------------------------------------

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    // -----------------------------------------------------------------------
    // Provider builder helpers
    // -----------------------------------------------------------------------

    private ClientRegistration buildGoogleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
    }
}

