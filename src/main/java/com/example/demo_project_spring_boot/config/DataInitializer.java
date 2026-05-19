package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "local"})
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-users:false}")
    private boolean seedDemoUsers;

    @Value("${app.seed-demo-admin-username:admin}")
    private String demoAdminUsername;

    @Value("${app.seed-demo-admin-email:admin@local.app}")
    private String demoAdminEmail;

    @Value("${app.seed-demo-admin-password:}")
    private String demoAdminPassword;

    @Value("${app.seed-demo-user-username:user}")
    private String demoUserUsername;

    @Value("${app.seed-demo-user-email:user@local.app}")
    private String demoUserEmail;

    @Value("${app.seed-demo-user-password:}")
    private String demoUserPassword;

    @Override
    public void run(String... args) {
        if (!seedDemoUsers) {
            log.info("Demo user seeding is disabled (app.seed-demo-users=false).");
            return;
        }

        if (demoAdminPassword.isBlank() || demoUserPassword.isBlank()) {
            log.warn("Demo user seeding requested but demo passwords are empty. Skipping seeding.");
            return;
        }

        // Create default Admin user if not exists
        if (userRepository.findByUsername(demoAdminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(demoAdminUsername)
                    .email(demoAdminEmail)
                    .password(passwordEncoder.encode(demoAdminPassword))
                    .role(Role.ADMIN)
                    .isEnabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Default demo admin user created: {}", demoAdminUsername);
        }

        // Create default User if not exists
        if (userRepository.findByUsername(demoUserUsername).isEmpty()) {
            User user = User.builder()
                    .username(demoUserUsername)
                    .email(demoUserEmail)
                    .password(passwordEncoder.encode(demoUserPassword))
                    .role(Role.USER)
                    .isEnabled(true)
                    .build();
            userRepository.save(user);
            log.info("Default demo user created: {}", demoUserUsername);
        }
    }
}
