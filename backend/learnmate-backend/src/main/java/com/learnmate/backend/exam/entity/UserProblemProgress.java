package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_problem_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_problem", columnNames = {"user_id", "problem_id"})
}, indexes = {
        @Index(name = "idx_upp_user", columnList = "user_id"),
        @Index(name = "idx_upp_problem", columnList = "problem_id")
})
public class UserProblemProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false)
    private boolean solved = false;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount = 0;

    @Column(name = "best_time_ms")
    private Integer bestTimeMs;

    @Column(name = "first_solved_at")
    private Instant firstSolvedAt;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem p) { this.problem = p; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean s) { this.solved = s; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int a) { this.attempts = a; }
    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int c) { this.acceptedCount = c; }
    public Integer getBestTimeMs() { return bestTimeMs; }
    public void setBestTimeMs(Integer t) { this.bestTimeMs = t; }
    public Instant getFirstSolvedAt() { return firstSolvedAt; }
    public void setFirstSolvedAt(Instant t) { this.firstSolvedAt = t; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant t) { this.lastAttemptAt = t; }
}
