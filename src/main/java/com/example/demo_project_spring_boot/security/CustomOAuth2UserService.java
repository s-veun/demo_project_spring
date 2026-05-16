package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String USER_ID_ATTRIBUTE = "app_user_id";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId().toLowerCase();
        AuthProvider provider = mapProvider(registrationId);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = extractEmail(provider, attributes);
        String providerId = extractProviderId(provider, attributes);
        String firstName = extractFirstName(attributes);
        String lastName = extractLastName(attributes);
        String profileImage = extractProfileImage(provider, attributes);

        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "Provider did not return user id");
        }

        if (email == null || email.isBlank()) {
            if (provider == AuthProvider.FACEBOOK) {
                email = "facebook_" + providerId + "@oauth.local";
            } else {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "Provider did not return email");
            }
        }

        User user = upsertSocialUser(provider, providerId, email, firstName, lastName, profileImage);

        Map<String, Object> normalizedAttributes = new java.util.HashMap<>(attributes);
        normalizedAttributes.put("provider", provider.name());
        normalizedAttributes.put("email", user.getEmail());
        normalizedAttributes.put(USER_ID_ATTRIBUTE, user.getId());

        log.info("Loaded OAuth2 user email={} provider={} appUserId={}", user.getEmail(), provider, user.getId());

        return new DefaultOAuth2User(
                List.of(() -> DEFAULT_ROLE),
                normalizedAttributes,
                resolvePrincipalNameAttribute(provider)
        );
    }

    private User upsertSocialUser(AuthProvider provider, String providerId, String email, String firstName, String lastName, String profileImage) {
        Optional<User> existingByProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        Optional<User> existingByEmail = userRepository.findByEmail(email);

        User user = existingByProvider
                .or(() -> existingByEmail)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .username(email)
                        .role(Role.USER)
                        .isEnabled(true)
                        .build());

        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setIsOAuth2Linked(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfileImageUrl(profileImage);
        user.setLastLoginAt(LocalDateTime.now());

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(email);
        }
        return userRepository.save(user);
    }

    private AuthProvider mapProvider(String registrationId) {
        return switch (registrationId) {
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"), "Unsupported provider: " + registrationId);
        };
    }

    private String extractEmail(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.FACEBOOK) {
            return asString(attributes.get("email"));
        }
        return asString(attributes.get("email"));
    }

    private String extractProviderId(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> asString(attributes.get("sub"));
            case FACEBOOK -> asString(attributes.get("id"));
            default -> null;
        };
    }

    private String extractFirstName(Map<String, Object> attributes) {
        return asString(attributes.getOrDefault("given_name", attributes.get("name")));
    }

    private String extractLastName(Map<String, Object> attributes) {
        return asString(attributes.get("family_name"));
    }

    private String extractProfileImage(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return asString(attributes.get("picture"));
        }

        if (provider == AuthProvider.FACEBOOK) {
            Object picture = attributes.get("picture");
            if (picture instanceof Map<?, ?> pictureMap) {
                Object data = pictureMap.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    return asString(dataMap.get("url"));
                }
            }
        }

        return null;
    }

    private String resolvePrincipalNameAttribute(AuthProvider provider) {
        return provider == AuthProvider.FACEBOOK ? "id" : "sub";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

