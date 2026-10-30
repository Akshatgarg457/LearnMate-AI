package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts", indexes = {
        @Index(name = "idx_qa_user", columnList = "user_id"),
        @Index(name = "idx_qa_quiz", columnList = "quiz_id")
})
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.IN_PROGRESS;

    /** null means untimed */
    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score")
    private Integer score;

    @Column(name = "max_score")
    private Integer maxScore;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    @PrePersist protected void onCreate() { startedAt = Instant.now(); }

    public enum Status { IN_PROGRESS, SUBMITTED, EXPIRED, TIMED_OUT }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz q) { this.quiz = q; }
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
    public List<QuizAnswer> getAnswers() { return answers; }
}
