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
 * Spring Boot's built-in {@code OAuth2ClientAutoConfiguration} validates at startup that
 * {@code client-id} is non-empty.  We exclude that autoconfiguration (see
 * {@code application.properties}) and build the {@link ClientRegistrationRepository} here,
 * only adding providers whose credentials are actually supplied via environment variables.
 * <p>
 * Required env vars:
 * <ul>
 *   <li>Google  — {@code GOOGLE_CLIENT_ID} + {@code GOOGLE_CLIENT_SECRET}</li>
 *   <li>Facebook — {@code FACEBOOK_APP_ID} + {@code FACEBOOK_APP_SECRET}</li>
 * </ul>
 * When none are set the application starts normally; social-login endpoints simply return
 * an appropriate error at runtime.
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
    // Facebook credentials  (env: FACEBOOK_APP_ID / FACEBOOK_APP_SECRET)
    // -----------------------------------------------------------------------
    @Value("${app.oauth2.facebook.client-id:}")
    private String facebookClientId;

    @Value("${app.oauth2.facebook.client-secret:}")
    private String facebookClientSecret;

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

        if (StringUtils.hasText(facebookClientId) && StringUtils.hasText(facebookClientSecret)) {
            registrations.add(buildFacebookRegistration());
            log.info("✅ Facebook OAuth2 client registered (clientId={}...)", facebookClientId.substring(0, Math.min(8, facebookClientId.length())));
        } else {
            log.warn("⚠️  FACEBOOK_APP_ID / FACEBOOK_APP_SECRET not set — Facebook login disabled.");
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
        return (StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret))
                || (StringUtils.hasText(facebookClientId) && StringUtils.hasText(facebookClientSecret));
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
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
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
                .scope("public_profile", "email")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email,picture")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}

