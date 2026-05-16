package com.example.demo_project_spring_boot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 Authentication Failure Handler
 * Handles failed OAuth2 login attempts
 * Redirects the user back to frontend callback page with OAuth2 error details.
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private OAuth2RedirectUriResolver redirectUriResolver;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        String frontendRedirectUri = redirectUriResolver.resolve(request);
        String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "oauth2_authentication_failed")
                .queryParam("message", exception.getMessage() != null ? exception.getMessage() : "OAuth2 authentication failed")
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }
}

