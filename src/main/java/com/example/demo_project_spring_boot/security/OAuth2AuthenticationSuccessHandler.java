package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth2 Authentication Success Handler
 * Handles successful OAuth2 login (e.g., Google Sign-in)
 * Creates or updates user in database and generates JWT token
 */
@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuth2RedirectUriResolver redirectUriResolver;

    @Autowired
    private TokenCookieService tokenCookieService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException {

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId()
                    .toLowerCase();
            AuthProvider provider = resolveProvider(registrationId);
            String email = asString(oauth2User.getAttributes().get("email"));
            String fullName = asString(oauth2User.getAttributes().get("name"));
            String picture = extractProfileImage(provider, oauth2User);
            String providerId = extractProviderId(provider, oauth2User);

            Object appUserId = oauth2User.getAttributes().get(CustomOAuth2UserService.USER_ID_ATTRIBUTE);
            User user = resolveAuthenticatedUser(appUserId, provider, providerId, email, fullName, picture);

            user.setLastLoginAt(LocalDateTime.now());
            if (user.getRole() == null) {
                user.setRole(Role.USER);
            }
            String accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getId());
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            userRepository.save(user);
            tokenCookieService.writeRefreshTokenCookie(response, refreshToken, jwtService.getRefreshTokenExpirationSeconds());

            String frontendRedirectUri = redirectUriResolver.resolve(request);

            String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("token", accessToken)
                    .queryParam("provider", provider.name())
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirect);
            log.info("OAuth2 authentication successful for userId={} provider={}", user.getId(), provider);

        } catch (Exception ex) {
            log.error("Error handling OAuth2 authentication success", ex);
            String frontendRedirectUri = redirectUriResolver.resolve(request);
            String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("error", "oauth2_processing_failed")
                    .queryParam("message", sanitizeErrorMessage(ex.getMessage()))
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirect);
        }
    }

    private User resolveAuthenticatedUser(Object appUserId,
                                          AuthProvider provider,
                                          String providerId,
                                          String email,
                                          String fullName,
                                          String picture) {
        if (appUserId != null) {
            try {
                Long userId = Long.parseLong(String.valueOf(appUserId));
                Optional<User> byId = userRepository.findById(userId);
                if (byId.isPresent()) {
                    return byId.get();
                }
                log.warn("OAuth2 principal contained app_user_id={} but no matching user found", appUserId);
            } catch (NumberFormatException ignored) {
                log.warn("Invalid app_user_id in OAuth2 principal: {}", appUserId);
            }
        }

        if (!StringUtils.hasText(email)) {
            if (provider == AuthProvider.FACEBOOK && StringUtils.hasText(providerId)) {
                email = "facebook_" + providerId + "@oauth.local";
            } else {
                throw new IllegalStateException("OAuth2 provider did not return email");
            }
        }
        final String resolvedEmail = email;

        Optional<User> existingByProvider = StringUtils.hasText(providerId)
                ? userRepository.findByProviderAndProviderId(provider, providerId)
                : Optional.empty();

        User user = existingByProvider
                .or(() -> userRepository.findByEmail(resolvedEmail))
                .orElseGet(() -> User.builder()
                        .email(resolvedEmail)
                        .username(resolvedEmail)
                        .role(Role.USER)
                        .isEnabled(true)
                        .build());

        user.setEmail(resolvedEmail);
        if (!StringUtils.hasText(user.getUsername())) {
            user.setUsername(email);
        }
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setIsOAuth2Linked(true);
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        user.setProfileImageUrl(picture);

        if (StringUtils.hasText(fullName)) {
            String[] tokens = fullName.trim().split("\\s+", 2);
            user.setFirstName(tokens[0]);
            if (tokens.length > 1) {
                user.setLastName(tokens[1]);
            }
        }

        return userRepository.save(user);
    }

    private String sanitizeErrorMessage(String message) {
        if (ObjectUtils.isEmpty(message)) {
            return "OAuth2 authentication failed";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private AuthProvider resolveProvider(String registrationId) {
        return switch (registrationId) {
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> throw new IllegalStateException("Unsupported OAuth2 provider: " + registrationId);
        };
    }

    private String extractProviderId(AuthProvider provider, OAuth2User oauth2User) {
        return switch (provider) {
            case GOOGLE -> asString(oauth2User.getAttributes().get("sub"));
            case FACEBOOK -> asString(oauth2User.getAttributes().get("id"));
            default -> null;
        };
    }

    private String extractProfileImage(AuthProvider provider, OAuth2User oauth2User) {
        if (provider == AuthProvider.GOOGLE) {
            return asString(oauth2User.getAttributes().get("picture"));
        }

        Object picture = oauth2User.getAttributes().get("picture");
        if (picture instanceof java.util.Map<?, ?> pictureMap) {
            Object data = pictureMap.get("data");
            if (data instanceof java.util.Map<?, ?> dataMap) {
                return asString(dataMap.get("url"));
            }
        }

        return null;
    }
}

