package com.learnmate.backend.progress.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One record per qualifying activity event. Used to compute streaks,
 * heatmaps, and XP. The server creates these — never the client.
 */
@Entity
@Table(name = "learning_activities", indexes = {
        @Index(name = "idx_la_user_date", columnList = "user_id, activity_date"),
        @Index(name = "idx_la_type", columnList = "activity_type")
})
public class LearningActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded = 0;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum ActivityType {
        CODE_SUBMISSION_ACCEPTED, QUIZ_COMPLETED, MOCK_EXAM_COMPLETED,
        PEER_SESSION_LEARNER, PEER_SESSION_TEACHER, NOTE_ADDED,
        COMMUNITY_ANSWER, AI_CONVERSATION
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType t) { this.activityType = t; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate d) { this.activityDate = d; }
    public int getXpAwarded() { return xpAwarded; }
    public void setXpAwarded(int x) { this.xpAwarded = x; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long r) { this.referenceId = r; }
    public Instant getCreatedAt() { return createdAt; }
}
