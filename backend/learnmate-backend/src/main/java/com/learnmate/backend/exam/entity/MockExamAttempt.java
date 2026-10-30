package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_exam_attempts", indexes = {
        @Index(name = "idx_mea_user", columnList = "user_id"),
        @Index(name = "idx_mea_exam", columnList = "exam_id")
})
public class MockExamAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private MockExam exam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score")
    private Integer score;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "pass_percentage")
    private Integer passPercentage;

    @Column(name = "passed")
    private Boolean passed;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockExamAnswer> answers = new ArrayList<>();

    @PrePersist protected void onCreate() { startedAt = Instant.now(); }

    public enum Status { IN_PROGRESS, SUBMITTED, TIMED_OUT }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public MockExam getExam() { return exam; }
    public void setExam(MockExam e) { this.exam = e; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant d) { this.deadlineAt = d; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant s) { this.submittedAt = s; }
    public Integer getScore() { return score; }
    public void setScore(Integer s) { this.score = s; }
    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer m) { this.maxScore = m; }
    public Integer getPassPercentage() { return passPercentage; }
    public void setPassPercentage(Integer p) { this.passPercentage = p; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean p) { this.passed = p; }
    public List<MockExamAnswer> getAnswers() { return answers; }
}
