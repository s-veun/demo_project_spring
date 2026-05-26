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

    /** Set via FRONTEND_URL or FRONTEND_BASE_URL env var on Railway. */
    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @Value("${app.oauth2.authorized-redirect-uri:}")
    private String authorizedRedirectUri;

    @Value("${app.oauth2.allowed-redirect-uris:}")
    private String allowedRedirectUris;

    public String resolve(HttpServletRequest request) {
        String requestedRedirect = request.getParameter(FRONTEND_REDIRECT_PARAM);

        if (StringUtils.hasText(requestedRedirect)) {
            if (isAllowed(requestedRedirect)) {
                log.debug("Using frontend_redirect_uri={}", requestedRedirect);
                return requestedRedirect;
            }
            log.warn("Rejected untrusted OAuth2 frontend_redirect_uri={}", requestedRedirect);
        }

        if (StringUtils.hasText(authorizedRedirectUri) && isAbsoluteHttpUrl(authorizedRedirectUri)) {
            return authorizedRedirectUri;
        }

        String base = resolvedFrontendBase();
        return base + "/auth/success";
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns true when the candidate URL is safe to redirect to.
     *
     * Candidate must be an absolute http/https URL and explicitly present in
     * the configured allow-list.
     */
    private boolean isAllowed(String candidate) {
        if (!StringUtils.hasText(candidate) || !isAbsoluteHttpUrl(candidate)) {
            return false;
        }

        Set<String> allowList = buildAllowList();

        if (allowList.isEmpty()) {
            log.warn("No OAuth2 redirect allow-list configured – rejecting frontend_redirect_uri. Configure FRONTEND_URL or OAUTH2_ALLOWED_REDIRECT_URIS.");
            return false;
        }

        return allowList.contains(candidate);
    }

    private Set<String> buildAllowList() {
        Set<String> list = new LinkedHashSet<>();
        appendCsvValues(list, allowedRedirectUris);
        appendCsvValues(list, authorizedRedirectUri);

        if (StringUtils.hasText(frontendUrl)) {
            String base = normalizeUrl(frontendUrl);
            list.add(base + "/auth/success");
            list.add(base + "/auth/callback");
        }

        return list;
    }

    /** Returns the configured frontend base URL, or localhost:3000 as a dev fallback. */
    private String resolvedFrontendBase() {
        if (StringUtils.hasText(frontendUrl)) {
            return normalizeUrl(frontendUrl);
        }
        if (StringUtils.hasText(authorizedRedirectUri) && isAbsoluteHttpUrl(authorizedRedirectUri)) {
            // Strip trailing path components to get the origin
            try {
                URI uri = URI.create(authorizedRedirectUri);
                return uri.getScheme() + "://" + uri.getHost() +
                        (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            } catch (Exception ignored) { /* fall through */ }
        }
        log.warn("app.frontend-url is not configured – defaulting redirect to http://localhost:3000/auth/success. " +
                 "Set FRONTEND_URL env var on Railway for production.");
        return "http://localhost:3000";
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
            return uri.getHost() != null &&
                    ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalizeUrl(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        return raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    }
}
