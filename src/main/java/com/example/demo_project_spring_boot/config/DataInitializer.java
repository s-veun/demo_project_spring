package com.example.demo_project_spring_boot.config;

import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default Admin user if not exists
        if (!userRepository.findByUsername("Savoeun").isPresent()) {
            User admin = User.builder()
                    .username("Savoeun")
                    .password(passwordEncoder.encode("Saveun2032"))
                    .role(Role.ADMIN)
                    .isEnabled(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Default Admin user created: Savoeun");
        }

        // Create default User if not exists
        if (!userRepository.findByUsername("user").isPresent()) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.USER)
                    .isEnabled(true)
                    .build();
            userRepository.save(user);
            log.info("✅ Default User created: user");
        }
    }
}
