package com.example.demo_project_spring_boot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "user_id"})
})
public class ReviewVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean isHelpful; // true = helpful, false = not helpful

    @Column(nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        this.votedAt = LocalDateTime.now();
    }
}
