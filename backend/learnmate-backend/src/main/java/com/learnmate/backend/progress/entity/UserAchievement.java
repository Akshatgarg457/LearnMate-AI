package com.learnmate.backend.progress.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_achievements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ua_user_achievement", columnNames = {"user_id","achievement_id"})
})
public class UserAchievement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private Instant earnedAt;

    @PrePersist protected void onCreate() { earnedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Achievement getAchievement() { return achievement; }
    public void setAchievement(Achievement a) { this.achievement = a; }
    public Instant getEarnedAt() { return earnedAt; }
}
