package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.RefreshTokenRequest;
import com.example.demo_project_spring_boot.dto.RefreshTokenResponse;
import com.example.demo_project_spring_boot.exception.BadRequestException;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .username("john")
                .email("john@example.com")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isEnabled(true)
                .refreshToken("refresh-token")
                .build();
    }

    @Test
    void refreshTokenReturnsConfiguredExpiration() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .build();

        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.isTokenExpired("refresh-token")).thenReturn(false);
        when(jwtService.extractUsername("refresh-token")).thenReturn("john");
        when(jwtService.extractUserId("refresh-token")).thenReturn(10L);
        when(jwtService.generateAccessToken(10L, "john", "john@example.com", "USER")).thenReturn("new-access");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        RefreshTokenResponse response = authenticationService.refreshToken(request);

        assertEquals(1800L, response.getExpiresIn());
        assertEquals("new-access", response.getAccessToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void refreshTokenRejectsBlankToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(" ").build();

        assertThrows(BadRequestException.class, () -> authenticationService.refreshToken(request));
    }
}

