package com.learnmate.backend.community.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_answers", indexes = {
        @Index(name = "idx_ca_question", columnList = "question_id"),
        @Index(name = "idx_ca_author", columnList = "author_id")
})
public class CommunityAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private CommunityQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Column(name = "is_accepted", nullable = false)
    private boolean accepted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.VISIBLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public enum Status { VISIBLE, REMOVED }

    public Long getId() { return id; }
    public CommunityQuestion getQuestion() { return question; }
    public void setQuestion(CommunityQuestion q) { this.question = q; }
    public User getAuthor() { return author; }
    public void setAuthor(User u) { this.author = u; }
    public String getBody() { return body; }
    public void setBody(String b) { this.body = b; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int v) { this.voteCount = v; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean a) { this.accepted = a; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
