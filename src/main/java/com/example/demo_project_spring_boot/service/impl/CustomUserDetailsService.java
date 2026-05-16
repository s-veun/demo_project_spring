package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(loginId)
                .or(() -> userRepository.findByEmail(loginId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

        if (user.getRole() == null) {
            // Treat broken user records as authentication failures instead of bubbling as 500 errors.
            throw new UsernameNotFoundException("User role is not configured: " + loginId);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword() == null ? "{noop}oauth2-user" : user.getPassword())
                .disabled(!Boolean.TRUE.equals(user.getIsEnabled()))
                .roles(user.getRole().name())
                .build();
    }
}