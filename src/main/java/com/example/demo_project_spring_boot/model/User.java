package com.example.demo_project_spring_boot.model;

import com.example.demo_project_spring_boot.Enum.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
    
    private String profileImageUrl;
    
    private String firstName;
    
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Role role;

    // ============= OAuth2 Fields =============
    @Column(name = "oauth_provider")
    private String provider; // LOCAL, GOOGLE, GITHUB, FACEBOOK, etc.

    @Column(name = "oauth_provider_id")
    private String providerId; // Google user ID or other OAuth provider ID

    @Builder.Default
    @Column(name = "oauth_account_linked", nullable = false)
    private Boolean isOAuth2Linked = false;

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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}