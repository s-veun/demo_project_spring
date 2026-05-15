package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.OAuth2LoginResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
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

    @Value("${app.oauth2.authorized-redirect-uri:}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication)
                    .getAuthorizedClientRegistrationId()
                    .toLowerCase();
            AuthProvider provider = registrationId.equals("facebook") ? AuthProvider.FACEBOOK : AuthProvider.GOOGLE;

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

            OAuth2LoginResponse loginResponse = OAuth2LoginResponse.builder()
                    .success(true)
                    .message("OAuth2 login successful")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .role(user.getRole().name())
                    .provider(provider.name())
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                    .build();

            if (StringUtils.hasText(authorizedRedirectUri)) {
                String redirect = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                        .queryParam("accessToken", accessToken)
                        .queryParam("refreshToken", refreshToken)
                        .queryParam("provider", provider.name())
                        .build()
                        .toUriString();
                getRedirectStrategy().sendRedirect(request, response, redirect);
                return;
            }

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new ObjectMapper().writeValueAsString(loginResponse));

            log.info("OAuth2 authentication successful for userId={} provider={}", user.getId(), provider);

        } catch (Exception ex) {
            log.error("Error handling OAuth2 authentication success", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 authentication failed");
        }
    }
}

