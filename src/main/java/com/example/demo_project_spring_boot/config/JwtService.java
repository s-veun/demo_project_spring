package com.example.demo_project_spring_boot.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
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
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Validate JWT token by username (useful for OAuth2)
     */
    public boolean isTokenValidByUsername(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
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
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}