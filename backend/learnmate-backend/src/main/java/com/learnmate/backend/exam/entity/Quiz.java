package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String subject;

    @Column(length = 100)
    private String topic;

    /** Default duration in minutes; 0 = untimed */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 15;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public String getTopic() { return topic; }
    public void setTopic(String t) { this.topic = t; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int d) { this.durationMinutes = d; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt() { return createdAt; }
    public List<QuizQuestion> getQuestions() { return questions; }
}
