package com.example.demo_project_spring_boot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Authentication Failure Handler
 * Handles failed OAuth2 login attempts
 * Returns JSON error response to frontend
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri:}")
    private String authorizedRedirectUri;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        String frontendRedirectUri = request.getParameter("frontend_redirect_uri");
        if (!StringUtils.hasText(frontendRedirectUri)) {
            frontendRedirectUri = StringUtils.hasText(authorizedRedirectUri)
                    ? authorizedRedirectUri
                    : frontendUrl + "/auth/success";
        }

        if (StringUtils.hasText(frontendRedirectUri)) {
            String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("error", "oauth2_authentication_failed")
                    .queryParam("message", exception.getMessage() != null ? exception.getMessage() : "OAuth2 authentication failed")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, redirect);
            return;
        }

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Authentication Failed");
        errorResponse.put("message", exception.getMessage() != null ?
                          exception.getMessage() : "OAuth2 authentication failed");
        errorResponse.put("timestamp", System.currentTimeMillis());

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }
}

