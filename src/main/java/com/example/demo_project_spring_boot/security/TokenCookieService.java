package com.example.demo_project_spring_boot.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenCookieService {

    private static final List<String> REFRESH_COOKIE_NAMES = List.of("refreshToken", "te_refresh_token");

    @Value("${app.auth.cookie.path:/}")
    private String cookiePath;

    @Value("${app.auth.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.auth.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.auth.cookie.same-site:None}")
    private String sameSite;

    public void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        if (response == null || refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        for (String cookieName : REFRESH_COOKIE_NAMES) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(cookieName, refreshToken, maxAgeSeconds).toString());
        }
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        for (String cookieName : REFRESH_COOKIE_NAMES) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(cookieName, "", 0).toString());
        }
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(Math.max(maxAgeSeconds, 0));

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }
}
