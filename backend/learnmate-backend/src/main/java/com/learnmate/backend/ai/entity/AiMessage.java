package com.learnmate.backend.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_messages")
public class AiMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum Role { USER, ASSISTANT }

    public Long getId() { return id; }
    public AiConversation getConversation() { return conversation; }
    public void setConversation(AiConversation c) { this.conversation = c; }
    public Role getRole() { return role; }
    public void setRole(Role r) { this.role = r; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public Instant getCreatedAt() { return createdAt; }
}
