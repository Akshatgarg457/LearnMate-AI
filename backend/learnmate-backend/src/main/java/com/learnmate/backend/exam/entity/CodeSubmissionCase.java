package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Per-test-case result. Inputs, expected outputs and actual outputs are only
 * persisted for sample cases — hidden case data never leaves the server.
 */
@Entity
@Table(name = "code_submission_cases", indexes = {
        @Index(name = "idx_sub_case_submission", columnList = "submission_id")
})
public class CodeSubmissionCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    @JsonIgnore
    private CodeSubmission submission;

    @Column(name = "case_index", nullable = false)
    private int caseIndex;

    @Column(nullable = false)
    private boolean sample;

    @Column(nullable = false)
    private boolean passed;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CodeSubmission.Status status;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    public Long getId() { return id; }
    public CodeSubmission getSubmission() { return submission; }
    public void setSubmission(CodeSubmission s) { this.submission = s; }
    public int getCaseIndex() { return caseIndex; }
    public void setCaseIndex(int i) { this.caseIndex = i; }
    public boolean isSample() { return sample; }
    public void setSample(boolean s) { this.sample = s; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean p) { this.passed = p; }
    public CodeSubmission.Status getStatus() { return status; }
    public void setStatus(CodeSubmission.Status s) { this.status = s; }
    public Integer getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Integer t) { this.executionTimeMs = t; }
    public Integer getMemoryUsedKb() { return memoryUsedKb; }
    public void setMemoryUsedKb(Integer m) { this.memoryUsedKb = m; }
    public String getInput() { return input; }
    public void setInput(String i) { this.input = i; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String e) { this.expectedOutput = e; }
    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String a) { this.actualOutput = a; }
    public String getStderr() { return stderr; }
    public void setStderr(String e) { this.stderr = e; }
}
