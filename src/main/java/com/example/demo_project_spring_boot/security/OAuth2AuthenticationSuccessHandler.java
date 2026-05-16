package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException {

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId()
                    .toLowerCase();
            if (!"google".equals(registrationId)) {
                throw new IllegalStateException("Unsupported OAuth2 provider: " + registrationId);
            }
            AuthProvider provider = AuthProvider.GOOGLE;

            Object appUserId = oauth2User.getAttributes().get(CustomOAuth2UserService.USER_ID_ATTRIBUTE);
            if (appUserId == null) {
                throw new IllegalStateException("OAuth2 app user id is missing in principal attributes");
            }

            Long userId = Long.parseLong(String.valueOf(appUserId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("OAuth2 authenticated user not found in database"));

            user.setLastLoginAt(LocalDateTime.now());
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

            String frontendRedirectUri = redirectUriResolver.resolve(request);

            String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", provider.name())
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirect);
            log.info("OAuth2 authentication successful for userId={} provider={}", user.getId(), provider);

        } catch (Exception ex) {
            log.error("Error handling OAuth2 authentication success", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 authentication failed");
        }
    }
}

