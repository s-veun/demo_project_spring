package com.example.demo_project_spring_boot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"railway", "prod"})
@Slf4j
public class RuntimeConfigValidator {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @Value("${app.security.strict-startup-validation:false}")
    private boolean strictStartupValidation;

    @Value("${RAILWAY_ENVIRONMENT:}")
    private String railwayEnvironment;

    @PostConstruct
    public void validate() {
        boolean strictMode = strictStartupValidation || StringUtils.hasText(railwayEnvironment);

        List<String> fatalErrors = new ArrayList<>();

        if (!StringUtils.hasText(datasourceUrl)) {
            fatalErrors.add("Database URL is missing. Set SPRING_DATASOURCE_URL.");
        }

        if (!isStrongJwtSecret(jwtSecret)) {
            fatalErrors.add("JWT_SECRET is missing or too weak. Provide at least 32 random characters.");
        }

        if (!fatalErrors.isEmpty()) {
            if (strictMode) {
                throw new IllegalStateException(String.join(" ", fatalErrors));
            }
            log.warn("Runtime validation warnings (non-strict mode): {}", String.join(" ", fatalErrors));
        }

        if (!StringUtils.hasText(frontendUrl)) {
            log.warn("FRONTEND_BASE_URL/FRONTEND_URL is not set. OAuth2 redirect validation may fail.");
        }
    }

    private boolean isStrongJwtSecret(String value) {
        return StringUtils.hasText(value)
                && value.trim().length() >= 32
                && !looksLikePlaceholder(value.trim());
    }

    private boolean looksLikePlaceholder(String value) {
        String normalized = value.toLowerCase();
        return normalized.contains("replace") || normalized.contains("change-this") || normalized.contains("your-");
    }
}

