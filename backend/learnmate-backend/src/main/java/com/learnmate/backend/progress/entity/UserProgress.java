package com.learnmate.backend.progress.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per user. Caches streak and XP state.
 * Updated transactionally alongside LearningActivity inserts.
 */
@Entity
@Table(name = "user_progress")
public class UserProgress {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_xp", nullable = false)
    private int totalXp = 0;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "next_level_xp", nullable = false)
    private int nextLevelXp = 100;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int c) { this.currentStreak = c; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int l) { this.longestStreak = l; }
    public LocalDate getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(LocalDate d) { this.lastActivityDate = d; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int x) { this.totalXp = x; }
    public int getLevel() { return level; }
    public void setLevel(int l) { this.level = l; }
    public int getNextLevelXp() { return nextLevelXp; }
    public void setNextLevelXp(int n) { this.nextLevelXp = n; }
    public Instant getUpdatedAt() { return updatedAt; }
}
