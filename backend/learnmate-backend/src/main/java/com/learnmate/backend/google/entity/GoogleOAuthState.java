package com.learnmate.backend.google.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One-time OAuth state value bound to the LearnMate user who started the flow.
 * Rows are deleted when the callback consumes them or the flow expires.
 */
@Entity
@Table(name = "google_oauth_states", indexes = {
        @Index(name = "idx_gos_state", columnList = "state_value", unique = true)
})
public class GoogleOAuthState {

    @Id
    @Column(name = "state_value", length = 128, nullable = false)
    private String state;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
