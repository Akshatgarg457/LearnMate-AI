package com.learnmate.backend.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversations", indexes = {
        @Index(name = "idx_aic_user", columnList = "user_id")
})
public class AiConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String subject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @JsonIgnore
    private List<AiMessage> messages = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AiMessage> getMessages() { return messages; }
}
