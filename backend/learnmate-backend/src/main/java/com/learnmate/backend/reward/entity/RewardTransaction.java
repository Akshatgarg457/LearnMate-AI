package com.learnmate.backend.reward.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable ledger entry. Every credit and debit is a separate row.
 * Balance = SUM(amount) for a user — negative amounts are debits.
 * idempotencyKey prevents duplicate awards from retries.
 */
@Entity
@Table(name = "reward_transactions", indexes = {
        @Index(name = "idx_rt_user", columnList = "user_id"),
        @Index(name = "idx_rt_type", columnList = "type"),
        @Index(name = "idx_rt_idem", columnList = "idempotency_key", unique = true)
})
public class RewardTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /** Positive = credit, negative = debit */
    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Unique key to prevent duplicate awards (e.g. "teaching-session-{id}") */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    @JsonIgnore
    private String idempotencyKey;

    /** Optional reference to the triggering entity */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum TransactionType {
        TEACHING_REWARD, STREAK_REWARD, MILESTONE_REWARD,
        COURSE_REDEMPTION, ADMIN_ADJUSTMENT, ADMIN_REVERSAL
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public int getAmount() { return amount; }
    public void setAmount(int a) { this.amount = a; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType t) { this.type = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String k) { this.idempotencyKey = k; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long r) { this.referenceId = r; }
    public Instant getCreatedAt() { return createdAt; }
}
