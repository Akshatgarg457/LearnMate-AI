package com.learnmate.backend.progress.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "achievements")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Achievement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementType type;

    /** Threshold value (e.g. streak days, submissions count) */
    @Column(name = "threshold_value", nullable = false)
    private int thresholdValue = 1;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 0;

    @Column(name = "points_reward", nullable = false)
    private int pointsReward = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum AchievementType {
        STREAK_DAYS, PROBLEMS_SOLVED, QUIZZES_COMPLETED,
        MOCK_EXAMS_PASSED, TEACHING_SESSIONS, COMMUNITY_ANSWERS,
        XP_MILESTONE, CUSTOM
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String i) { this.iconUrl = i; }
    public AchievementType getType() { return type; }
    public void setType(AchievementType t) { this.type = t; }
    public int getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(int t) { this.thresholdValue = t; }
    public int getXpReward() { return xpReward; }
    public void setXpReward(int x) { this.xpReward = x; }
    public int getPointsReward() { return pointsReward; }
    public void setPointsReward(int p) { this.pointsReward = p; }
    public Instant getCreatedAt() { return createdAt; }
}
