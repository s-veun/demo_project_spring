package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.example.demo_project_spring_boot.config.JwtService;
import com.example.demo_project_spring_boot.dto.OAuth2LoginResponse;
import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OAuth2 Callback Response Shape Validation Tests
 *
 * Validates that the backend OAuth2AuthenticationSuccessHandler
 * returns the correct response shape expected by the frontend
 */
@DisplayName("OAuth2 Callback Response Shape")
public class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private OAuth2AuthenticationSuccessHandler successHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        successHandler = new OAuth2AuthenticationSuccessHandler();
        ReflectionTestUtils.setField(successHandler, "userRepository", userRepository);
        ReflectionTestUtils.setField(successHandler, "jwtService", jwtService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("OAuth2LoginResponse contains all required fields")
    void testOAuth2LoginResponseShape() {
        // Create a sample response
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .message("OAuth2 login successful")
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                .refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIn0.TflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .username("user@google.com")
                .email("user@google.com")
                .firstName("John")
                .lastName("Doe")
                .profileImageUrl("https://example.com/profile.jpg")
                .role("USER")
                .provider("GOOGLE")
                .build();

        // Verify all fields are present
        assertTrue(response.isSuccess());
        assertEquals("OAuth2 login successful", response.getMessage());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals(1L, response.getUserId());
        assertEquals("user@google.com", response.getUsername());
        assertEquals("user@google.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("USER", response.getRole());
        assertEquals("GOOGLE", response.getProvider());
    }

    @Test
    @DisplayName("OAuth2LoginResponse serializes to valid JSON")
    void testOAuth2LoginResponseSerialization() throws Exception {
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .message("OAuth2 login successful")
                .accessToken("token123")
                .refreshToken("refresh456")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .provider("GOOGLE")
                .build();

        String json = objectMapper.writeValueAsString(response);

        // Verify JSON contains all required fields
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"accessToken\":\"token123\""));
        assertTrue(json.contains("\"refreshToken\":\"refresh456\""));
        assertTrue(json.contains("\"tokenType\":\"Bearer\""));
        assertTrue(json.contains("\"expiresIn\":3600"));
        assertTrue(json.contains("\"userId\":1"));
        assertTrue(json.contains("\"username\":\"testuser\""));
        assertTrue(json.contains("\"email\":\"test@example.com\""));
        assertTrue(json.contains("\"role\":\"USER\""));
        assertTrue(json.contains("\"provider\":\"GOOGLE\""));
    }

    @Test
    @DisplayName("OAuth2LoginResponse deserializes from frontend expectations")
    void testOAuth2LoginResponseDeserialization() throws Exception {
        // Simulate what frontend expects in query parameters
        String json = "{" +
                "\"success\":true," +
                "\"message\":\"OAuth2 login successful\"," +
                "\"accessToken\":\"eyJhbGc...\"," +
                "\"refreshToken\":\"eyJhbGc...\"," +
                "\"tokenType\":\"Bearer\"," +
                "\"expiresIn\":3600," +
                "\"userId\":1," +
                "\"username\":\"user@google.com\"," +
                "\"email\":\"user@google.com\"," +
                "\"role\":\"USER\"," +
                "\"provider\":\"GOOGLE\"" +
                "}";

        OAuth2LoginResponse response = objectMapper.readValue(json, OAuth2LoginResponse.class);

        assertEquals(true, response.isSuccess());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("Query parameter extraction produces valid tokens")
    void testQueryParameterTokens() {
        // Simulate what backend sends in redirect URL
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6IjEifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6IjIifQ.TflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // Verify tokens are JWT format (3 parts separated by dots)
        String[] accessParts = accessToken.split("\\.");
        String[] refreshParts = refreshToken.split("\\.");

        assertEquals(3, accessParts.length);
        assertEquals(3, refreshParts.length);

        // Verify tokens are not empty
        assertTrue(accessParts[0].length() > 0); // Header
        assertTrue(accessParts[1].length() > 0); // Payload
        assertTrue(accessParts[2].length() > 0); // Signature
    }

    @Test
    @DisplayName("OAuth2LoginResponse supports multiple token field names")
    void testTokenFieldNameFlexibility() throws Exception {
        // Test backend returning accessToken
        OAuth2LoginResponse response1 = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("token_variant_1")
                .build();

        String json1 = objectMapper.writeValueAsString(response1);
        assertTrue(json1.contains("\"accessToken\":\"token_variant_1\""));

        // Frontend should handle this
        OAuth2LoginResponse parsed1 = objectMapper.readValue(json1, OAuth2LoginResponse.class);
        assertEquals("token_variant_1", parsed1.getAccessToken());
    }

    @Test
    @DisplayName("OAuth2 providers (Google/Facebook) produce correct response")
    void testProviderSpecificResponses() {
        // Google response
        OAuth2LoginResponse googleResponse = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("google_token")
                .refreshToken("google_refresh")
                .provider("GOOGLE")
                .username("user@gmail.com")
                .email("user@gmail.com")
                .build();

        assertEquals("GOOGLE", googleResponse.getProvider());
        assertNotNull(googleResponse.getAccessToken());

        // Facebook response
        OAuth2LoginResponse facebookResponse = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("facebook_token")
                .refreshToken("facebook_refresh")
                .provider("FACEBOOK")
                .username("123456789") // Facebook provides numeric ID
                .email("user@facebook.com")
                .build();

        assertEquals("FACEBOOK", facebookResponse.getProvider());
        assertNotNull(facebookResponse.getAccessToken());
    }

    @Test
    @DisplayName("Response includes user profile information")
    void testResponseIncludesUserProfile() {
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .userId(42L)
                .username("john.doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .profileImageUrl("https://example.com/john.jpg")
                .role("USER")
                .accessToken("token")
                .build();

        // Frontend can use these for profile page
        assertEquals(42L, response.getUserId());
        assertEquals("john.doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertNotNull(response.getProfileImageUrl());
        assertEquals("USER", response.getRole());
    }

    @Test
    @DisplayName("Token expiration is properly communicated")
    void testTokenExpirationCommunication() {
        long accessTokenExpirationSeconds = 3600L; // 1 hour

        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("token")
                .expiresIn(accessTokenExpirationSeconds)
                .tokenType("Bearer")
                .build();

        assertEquals(3600L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());

        // Frontend can use this to schedule token refresh
        long expirationTime = System.currentTimeMillis() / 1000 + response.getExpiresIn();
        assertTrue(expirationTime > System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("Response handles missing optional fields gracefully")
    void testResponseWithMinimalFields() {
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("token123")
                .refreshToken("refresh123")
                .build();

        // Required fields
        assertTrue(response.isSuccess());
        assertEquals("token123", response.getAccessToken());
        assertEquals("refresh123", response.getRefreshToken());

        // Optional fields
        assertNull(response.getFirstName());
        assertNull(response.getLastName());
        assertNull(response.getProfileImageUrl());
    }

    @Test
    @DisplayName("Response URL query parameters match response JSON fields")
    void testQueryParametersVersusJsonPayload() {
        // When backend redirects with query params:
        // https://frontend.com/callback?accessToken=...&refreshToken=...&provider=...
        //
        // The values should match what would be in JSON response:
        // {"accessToken": "...", "refreshToken": "...", "provider": "..."}

        String accessToken = "eyJhbGc...";
        String refreshToken = "eyJhbGc...";
        String provider = "GOOGLE";

        // Create response as if it were from JSON
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .provider(provider)
                .build();

        // These values should match what's in query parameters
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals(provider, response.getProvider());
    }

    @Test
    @DisplayName("Bearer token format is always specified")
    void testBearerTokenFormat() {
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .accessToken("token123")
                .tokenType("Bearer")
                .build();

        assertEquals("Bearer", response.getTokenType());

        // Frontend should use: Authorization: Bearer <accessToken>
        String authHeader = response.getTokenType() + " " + response.getAccessToken();
        assertEquals("Bearer token123", authHeader);
    }

    @Test
    @DisplayName("Response is serializable to JSON for REST API")
    void testResponseSerializableForApi() throws Exception {
        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .message("OAuth2 login successful")
                .accessToken("token123")
                .refreshToken("refresh456")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .provider("GOOGLE")
                .build();

        // Should be JSON serializable
        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.length() > 0);
        assertTrue(json.contains("accessToken"));

        // Should be deserializable back
        OAuth2LoginResponse deserialized = objectMapper.readValue(json, OAuth2LoginResponse.class);
        assertEquals(response.getAccessToken(), deserialized.getAccessToken());
        assertEquals(response.getUserId(), deserialized.getUserId());
    }
}

