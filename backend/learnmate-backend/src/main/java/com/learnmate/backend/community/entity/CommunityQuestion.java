package com.learnmate.backend.community.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_questions", indexes = {
        @Index(name = "idx_cq_author", columnList = "author_id"),
        @Index(name = "idx_cq_status", columnList = "status"),
        @Index(name = "idx_cq_subject", columnList = "subject")
})
public class CommunityQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User author;

    @Column(nullable = false, length = 400)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CommunityAnswer> answers = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public enum Status { OPEN, CLOSED, REMOVED }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public void setAuthor(User u) { this.author = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getBody() { return body; }
    public void setBody(String b) { this.body = b; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public String getTopic() { return topic; }
    public void setTopic(String t) { this.topic = t; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int v) { this.voteCount = v; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int v) { this.viewCount = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CommunityAnswer> getAnswers() { return answers; }

    /** Number of answers, for the question feed. The answers themselves are fetched from their own endpoint. */
    @JsonProperty("answerCount")
    public int getAnswerCount() {
        return answers == null ? 0 : answers.size();
    }
}
