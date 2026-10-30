package com.learnmate.backend.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "course_redemptions", indexes = {
        @Index(name = "idx_cr_user", columnList = "user_id"),
        @Index(name = "idx_cr_course", columnList = "course_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cr_user_course", columnNames = {"user_id","course_id"})
})
public class CourseRedemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    @PrePersist protected void onCreate() { redeemedAt = Instant.now(); }

    public enum Status { ACTIVE, REVOKED }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Course getCourse() { return course; }
    public void setCourse(Course c) { this.course = c; }
    public int getPointsSpent() { return pointsSpent; }
    public void setPointsSpent(int p) { this.pointsSpent = p; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Instant getRedeemedAt() { return redeemedAt; }
}
