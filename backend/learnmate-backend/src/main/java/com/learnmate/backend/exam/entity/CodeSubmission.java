package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_submissions", indexes = {
        @Index(name = "idx_sub_user", columnList = "user_id"),
        @Index(name = "idx_sub_problem", columnList = "problem_id"),
        @Index(name = "idx_sub_status", columnList = "status")
})
public class CodeSubmission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    /** Judge-assigned token for polling */
    @Column(name = "judge_token", length = 100)
    @JsonIgnore
    private String judgeToken;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "total_tests")
    private Integer totalTests;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "judged_at")
    private Instant judgedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("caseIndex ASC")
    private List<CodeSubmissionCase> caseResults = new ArrayList<>();

    @PrePersist protected void onCreate() { submittedAt = Instant.now(); }

    public enum Language { JAVA, PYTHON, CPP, JAVASCRIPT }

    public enum Status {
        PENDING, PROCESSING, ACCEPTED, WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED,
        COMPILATION_ERROR, RUNTIME_ERROR, JUDGE_ERROR
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem p) { this.problem = p; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language l) { this.language = l; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public String getJudgeToken() { return judgeToken; }
    public void setJudgeToken(String t) { this.judgeToken = t; }
    public Integer getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Integer t) { this.executionTimeMs = t; }
    public Integer getMemoryUsedKb() { return memoryUsedKb; }
    public void setMemoryUsedKb(Integer m) { this.memoryUsedKb = m; }
    public Integer getPassedTests() { return passedTests; }
    public void setPassedTests(Integer p) { this.passedTests = p; }
    public Integer getTotalTests() { return totalTests; }
    public void setTotalTests(Integer t) { this.totalTests = t; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String e) { this.errorMessage = e; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getJudgedAt() { return judgedAt; }
    public void setJudgedAt(Instant j) { this.judgedAt = j; }
    public List<CodeSubmissionCase> getCaseResults() { return caseResults; }
}
