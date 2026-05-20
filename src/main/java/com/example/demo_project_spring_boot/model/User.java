package com.example.demo_project_spring_boot.model;

import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.Enum.AuthProvider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

    @Column(name = "profile_image")
    private String profileImageUrl;

    @Column(name = "profile_image_name")
    private String profileImageName;

    private String fullName;

    @Column(length = 1200)
    private String bio;

    private String gender;

    private LocalDate dateOfBirth;

    private String country;

    private String city;

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean phoneVerified = false;

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime phoneVerifiedAt;

    private String facebookUrl;

    private String telegramHandle;

    private String instagramUrl;

    private String linkedInUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailNotificationsEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean smsNotificationsEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean marketingNotificationsEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean securityAlertsEnabled = true;

    private String firstName;
    
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ============= OAuth2 Fields =============
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false)
    private AuthProvider provider;

    @Column(name = "oauth_provider_id")
    private String providerId; // Google user ID or other OAuth provider ID

    @Builder.Default
    @Column(name = "oauth_account_linked", nullable = false)
    private Boolean isOAuth2Linked = false;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    // ============= Relationships =============
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Address> addresses;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Order> orders;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private Cart cart;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Review> reviews;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Wishlist> wishlists;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEnabled = true;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        if (this.isEnabled == null) this.isEnabled = true;
        if (this.isOAuth2Linked == null) this.isOAuth2Linked = false;
        if (this.provider == null) this.provider = AuthProvider.LOCAL;
        if (this.emailVerified == null) this.emailVerified = false;
        if (this.phoneVerified == null) this.phoneVerified = false;
        if (this.emailNotificationsEnabled == null) this.emailNotificationsEnabled = true;
        if (this.smsNotificationsEnabled == null) this.smsNotificationsEnabled = false;
        if (this.marketingNotificationsEnabled == null) this.marketingNotificationsEnabled = false;
        if (this.securityAlertsEnabled == null) this.securityAlertsEnabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getProfileImageName() {
        return profileImageName;
    }

    public void setProfileImageName(String profileImageName) {
        this.profileImageName = profileImageName;
    }

    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}