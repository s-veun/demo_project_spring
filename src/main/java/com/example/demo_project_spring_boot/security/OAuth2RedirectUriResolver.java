package com.example.demo_project_spring_boot.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@Slf4j
public class OAuth2RedirectUriResolver {

    private static final String FRONTEND_REDIRECT_PARAM = "frontend_redirect_uri";

    @Value("${app.frontend-url:https://your-frontend-domain.com}")
    private String frontendUrl;

    @Value("${app.oauth2.authorized-redirect-uri:}")
    private String authorizedRedirectUri;

    @Value("${app.oauth2.allowed-redirect-uris:}")
    private String allowedRedirectUris;

    public String resolve(HttpServletRequest request) {
        String requestedRedirect = request.getParameter(FRONTEND_REDIRECT_PARAM);
        if (StringUtils.hasText(requestedRedirect) && isAllowed(requestedRedirect)) {
            return requestedRedirect;
        }

        if (StringUtils.hasText(requestedRedirect)) {
            log.warn("Rejected untrusted OAuth2 frontend_redirect_uri={}", requestedRedirect);
        }

        if (StringUtils.hasText(authorizedRedirectUri) && isAllowed(authorizedRedirectUri)) {
            return authorizedRedirectUri;
        }

        return normalizeBaseUrl(frontendUrl) + "/auth/success";
    }

    private boolean isAllowed(String candidate) {
        if (!StringUtils.hasText(candidate) || !isAbsoluteHttpUrl(candidate)) {
            return false;
        }

        Set<String> allowList = new LinkedHashSet<>();
        appendCsvValues(allowList, allowedRedirectUris);
        appendCsvValues(allowList, authorizedRedirectUri);

        if (StringUtils.hasText(frontendUrl)) {
            String normalizedBase = normalizeBaseUrl(frontendUrl);
            allowList.add(normalizedBase + "/auth/success");
            allowList.add(normalizedBase + "/auth/callback");
        }

        return allowList.contains(candidate);
    }

    private void appendCsvValues(Set<String> destination, String rawCsv) {
        if (!StringUtils.hasText(rawCsv)) {
            return;
        }

        for (String value : rawCsv.split(",")) {
            String trimmed = value.trim();
            if (StringUtils.hasText(trimmed) && isAbsoluteHttpUrl(trimmed)) {
                destination.add(trimmed);
            }
        }
    }

    private boolean isAbsoluteHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalizeBaseUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "https://your-frontend-domain.com";
        }
        return raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    }
}

