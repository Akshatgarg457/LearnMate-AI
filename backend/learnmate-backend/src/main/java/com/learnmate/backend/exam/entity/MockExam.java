package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_exams")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MockExam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(length = 100)
    private String subject;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "pass_percentage", nullable = false)
    private int passPercentage = 50;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<MockExamQuestion> questions = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String i) { this.instructions = i; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int d) { this.durationMinutes = d; }
    public int getPassPercentage() { return passPercentage; }
    public void setPassPercentage(int p) { this.passPercentage = p; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt() { return createdAt; }
    public List<MockExamQuestion> getQuestions() { return questions; }
}
