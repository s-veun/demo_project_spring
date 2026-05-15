package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.OAuth2LoginResponse;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.Enum.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        try {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            // Extract user information from OAuth2 provider
            String email = oidcUser.getEmail();
            String firstName = oidcUser.getGivenName();
            String lastName = oidcUser.getFamilyName();
            String profileImageUrl = oidcUser.getPicture();
            String providerId = oidcUser.getSubject(); // Google sub claim

            log.info("OAuth2 Login - Email: {}, FirstName: {}, LastName: {}", email, firstName, lastName);

            // Check if user exists by email
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                // User exists, update OAuth2 information
                user = existingUser.get();
                user.setProvider("GOOGLE");
                user.setProviderId(providerId);
                user.setIsOAuth2Linked(true);
                log.info("Existing user found, updating OAuth2 information for email: {}", email);
            } else {
                // Create new user from OAuth2 information
                user = User.builder()
                        .email(email)
                        .username(email) // Use email as username for OAuth2 users
                        .firstName(firstName)
                        .lastName(lastName)
                        .profileImageUrl(profileImageUrl)
                        .provider("GOOGLE")
                        .providerId(providerId)
                        .isOAuth2Linked(true)
                        .role(Role.USER)
                        .isEnabled(true)
                        .password("") // OAuth2 users don't have password
                        .build();
                log.info("Creating new user for OAuth2 email: {}", email);
            }

            // Save or update user in database
            user = userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getId());

            // Prepare response
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
                    .provider("GOOGLE")
                    .build();

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);

            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(loginResponse));

            log.info("OAuth2 authentication successful for user: {}", user.getId());

        } catch (Exception ex) {
            log.error("Error handling OAuth2 authentication success", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 authentication failed");
        }
    }
}

