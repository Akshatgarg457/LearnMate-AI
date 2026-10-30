package com.learnmate.backend.google.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Google OAuth credentials for one LearnMate user. Token values are stored
 * encrypted (AES-GCM) and are never serialized or returned by a controller.
 */
@Entity
@Table(name = "google_oauth_tokens", indexes = {
        @Index(name = "idx_got_user", columnList = "user_id", unique = true)
})
public class GoogleOAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "access_token_enc", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc;

    @Column(length = 512)
    private String scope;

    @Column(name = "token_type", length = 40)
    private String tokenType;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccessTokenEnc() { return accessTokenEnc; }
    public void setAccessTokenEnc(String accessTokenEnc) { this.accessTokenEnc = accessTokenEnc; }
    public String getRefreshTokenEnc() { return refreshTokenEnc; }
    public void setRefreshTokenEnc(String refreshTokenEnc) { this.refreshTokenEnc = refreshTokenEnc; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
