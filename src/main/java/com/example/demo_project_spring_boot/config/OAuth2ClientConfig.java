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
    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String googleClientSecret;

    @Value("${FACEBOOK_APP_ID:}")
    private String facebookClientId;

    @Value("${FACEBOOK_APP_SECRET:}")
    private String facebookClientSecret;

    // -----------------------------------------------------------------------
    // ClientRegistrationRepository — only includes providers with credentials
    // -----------------------------------------------------------------------

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret)) {
            registrations.add(buildGoogleRegistration());
            log.info("Google OAuth2 client registered.");
        } else {
            log.warn("GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET not set. Google login is disabled.");
        }

        if (StringUtils.hasText(facebookClientId) && StringUtils.hasText(facebookClientSecret)) {
            registrations.add(buildFacebookRegistration());
            log.info("Facebook OAuth2 client registered.");
        } else {
            log.warn("FACEBOOK_APP_ID / FACEBOOK_APP_SECRET not set. Facebook login is disabled.");
        }

        if (registrations.isEmpty()) {
            log.warn("No OAuth2 providers configured. Social login is disabled.");
            // Return a no-op repo so SecurityConfig can still call oauth2Login() safely.
            // At runtime, any attempt to initiate OAuth2 flow will receive a 401.
            return registrationId -> null;
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    /** True when at least one provider has credentials; used by SecurityConfig. */
    public boolean hasAnyRegistration() {
        boolean hasGoogle = StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret);
        boolean hasFacebook = StringUtils.hasText(facebookClientId) && StringUtils.hasText(facebookClientSecret);
        return hasGoogle || hasFacebook;
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

    private ClientRegistration buildFacebookRegistration() {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(facebookClientId)
                .clientSecret(facebookClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v19.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v19.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email,picture")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}

