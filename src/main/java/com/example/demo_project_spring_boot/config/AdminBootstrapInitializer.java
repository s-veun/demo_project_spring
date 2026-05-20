package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AdminBootstrapInitializer — runs in ALL profiles.
 *
 * Creates the first ADMIN account from environment variables when NO admin exists.
 * Set the following env vars to enable bootstrapping:
 *   ADMIN_BOOTSTRAP_USERNAME  (default: none — skipped if blank)
 *   ADMIN_BOOTSTRAP_EMAIL     (default: none — skipped if blank)
 *   ADMIN_BOOTSTRAP_PASSWORD  (default: none — skipped if blank)
 *
 * Once at least one ADMIN user exists, this initializer does nothing (idempotent).
 */
@Component
@Order(50)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${app.admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        // If no bootstrap credentials configured, skip silently.
        if (!StringUtils.hasText(bootstrapUsername)
                || !StringUtils.hasText(bootstrapEmail)
                || !StringUtils.hasText(bootstrapPassword)) {
            log.debug("[AdminBootstrap] Bootstrap credentials not configured — skipping admin seed.");
            return;
        }

        // Only create if no admin exists yet (first-run).
        long existingAdminCount = userRepository.countByRole(Role.ADMIN);
        if (existingAdminCount > 0) {
            log.debug("[AdminBootstrap] {} admin account(s) already exist — skipping seed.", existingAdminCount);
            return;
        }

        if (userRepository.findByUsername(bootstrapUsername).isPresent()) {
            log.warn("[AdminBootstrap] Username '{}' already exists but has no ADMIN role — skipping.", bootstrapUsername);
            return;
        }

        if (userRepository.existsByEmail(bootstrapEmail)) {
            log.warn("[AdminBootstrap] Email '{}' already registered — skipping admin seed.", bootstrapEmail);
            return;
        }

        User admin = User.builder()
                .username(bootstrapUsername.trim())
                .email(bootstrapEmail.trim())
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .isOAuth2Linked(false)
                .isEnabled(true)
                .build();
        userRepository.save(admin);
        log.info("[AdminBootstrap] First admin account created: username={}, email={}", bootstrapUsername, bootstrapEmail);
    }
}

