package com.example.demo_project_spring_boot.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for generating and validating JWT tokens
 * Supports both Access Token and Refresh Token generation
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Supported values: plain, base64, base64url, auto
     * - plain: raw UTF-8 secret bytes
     * - base64: RFC4648 Base64 secret bytes
     * - base64url: URL-safe Base64 secret bytes
     * - auto: detect format using safe heuristics
     */
    @Value("${app.jwt.secret-format:auto}")
    private String secretFormat;

    @Value("${app.jwt.access-token-expiration:86400000}") // 24 hours
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract email from JWT token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extract user ID from JWT token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return Long.parseLong(userId.toString());
        });
    }

    /**
     * Generate Access Token for UserDetails
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        var roles = userDetails.getAuthorities()
                .stream()
                .map(auth -> auth.getAuthority())
                .toList();
        extraClaims.put("roles", roles);
        extraClaims.put("type", "ACCESS");
        return generateToken(extraClaims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Generate Access Token with userId and email
     */
    public String generateAccessToken(Long userId, String username, String email, String roles) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("email", email);
        extraClaims.put("roles", roles);
        extraClaims.put("type", "ACCESS");
        return generateToken(extraClaims, username, accessTokenExpiration);
    }

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("type", "REFRESH");
        return generateToken(extraClaims, username, refreshTokenExpiration);
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equalsIgnoreCase(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equalsIgnoreCase(extractTokenType(token));
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration / 1000;
    }

    /**
     * Generate JWT Token with custom claims and expiration
     */
    private String generateToken(Map<String, Object> extraClaims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate JWT token against UserDetails
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username != null
                && username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    /**
     * Validate JWT token by username (useful for OAuth2)
     */
    public boolean isTokenValidByUsername(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername != null
                && extractedUsername.equals(username)
                && !isTokenExpired(token);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extract token expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key from secret
     */
    private Key getSignInKey() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        final String normalizedSecret = jwtSecret.trim();
        byte[] keyBytes = decodeSecretBytes(normalizedSecret, secretFormat);

        // Ensure minimum key size for HS256.
        if (keyBytes.length < 32) {
            keyBytes = sha256(normalizedSecret);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecretBytes(String secret, String format) {
        String normalizedFormat = format == null ? "auto" : format.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedFormat) {
            case "plain" -> secret.getBytes(StandardCharsets.UTF_8);
            case "base64" -> decodeBase64Strict(secret);
            case "base64url" -> decodeBase64UrlStrict(secret);
            case "auto" -> decodeAuto(secret);
            default -> throw new IllegalStateException(
                    "Unsupported app.jwt.secret-format='" + format + "'. Use plain|base64|base64url|auto");
        };
    }

    private byte[] decodeAuto(String secret) {
        // Explicit prefixes remove ambiguity in environments like Railway.
        if (secret.startsWith("base64:")) {
            return decodeBase64Strict(secret.substring("base64:".length()));
        }
        if (secret.startsWith("base64url:")) {
            return decodeBase64UrlStrict(secret.substring("base64url:".length()));
        }

        // Try strict Base64 only when the input looks like padded Base64.
        if (looksLikeStandardBase64(secret)) {
            try {
                return decodeBase64Strict(secret);
            } catch (IllegalStateException ignored) {
                // Fall through to plain text.
            }
        }

        // Try URL-safe Base64 only when it clearly looks URL-safe.
        if (looksLikeBase64Url(secret)) {
            try {
                return decodeBase64UrlStrict(secret);
            } catch (IllegalStateException ignored) {
                // Fall through to plain text.
            }
        }

        // Default to raw UTF-8 bytes for plain text secrets.
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] decodeBase64Strict(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT secret is not valid standard Base64. If your secret is plain text, set app.jwt.secret-format=plain.",
                    ex
            );
        }
    }

    private byte[] decodeBase64UrlStrict(String secret) {
        try {
            return Base64.getUrlDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT secret is not valid Base64URL. If your secret is plain text, set app.jwt.secret-format=plain.",
                    ex
            );
        }
    }

    private boolean looksLikeStandardBase64(String value) {
        return value.length() % 4 == 0 && value.matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    private boolean looksLikeBase64Url(String value) {
        // URL-safe Base64 typically has '-' and '_' and may omit '=' padding.
        return value.matches("^[A-Za-z0-9_-]+={0,2}$");
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}