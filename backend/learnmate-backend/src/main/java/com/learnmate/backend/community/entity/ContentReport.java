package com.learnmate.backend.community.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "content_reports", indexes = {
        @Index(name = "idx_report_reporter", columnList = "reporter_id"),
        @Index(name = "idx_report_status", columnList = "status")
})
public class ContentReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum ContentType { QUESTION, ANSWER, PEER_SESSION }
    public enum Status { PENDING, REVIEWED, DISMISSED, ACTIONED }

    public Long getId() { return id; }
    public User getReporter() { return reporter; }
    public void setReporter(User u) { this.reporter = u; }
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType t) { this.contentType = t; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long c) { this.contentId = c; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Long getReviewedById() { return reviewedById; }
    public void setReviewedById(Long r) { this.reviewedById = r; }
    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String n) { this.reviewerNotes = n; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant r) { this.reviewedAt = r; }
}
