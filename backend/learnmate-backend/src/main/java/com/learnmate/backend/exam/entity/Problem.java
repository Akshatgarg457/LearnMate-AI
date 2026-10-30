package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems", indexes = {
        @Index(name = "idx_problems_topic", columnList = "topic"),
        @Index(name = "idx_problems_difficulty", columnList = "difficulty"),
        @Index(name = "idx_problems_active", columnList = "active")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Problem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable identifier used by the seeder and frontend links. */
    @Column(unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(length = 100)
    private String topic;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "constraints", columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs = 2000;

    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb = 256;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<TestCase> testCases = new ArrayList<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProblemStarterCode> starterCodes = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public enum Difficulty { EASY, MEDIUM, HARD }

    // getters/setters
    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public String getTopic() { return topic; }
    public void setTopic(String t) { this.topic = t; }
    public String getTags() { return tags; }
    public void setTags(String t) { this.tags = t; }
    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String f) { this.inputFormat = f; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String f) { this.outputFormat = f; }
    public String getConstraints() { return constraints; }
    public void setConstraints(String c) { this.constraints = c; }
    public int getTimeLimitMs() { return timeLimitMs; }
    public void setTimeLimitMs(int t) { this.timeLimitMs = t; }
    public int getMemoryLimitMb() { return memoryLimitMb; }
    public void setMemoryLimitMb(int m) { this.memoryLimitMb = m; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TestCase> getTestCases() { return testCases; }
    public List<ProblemStarterCode> getStarterCodes() { return starterCodes; }
}
