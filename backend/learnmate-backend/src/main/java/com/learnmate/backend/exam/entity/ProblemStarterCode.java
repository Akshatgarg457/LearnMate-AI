package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "problem_starter_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_starter_problem_language", columnNames = {"problem_id", "language"})
})
public class ProblemStarterCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    @JsonIgnore
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CodeSubmission.Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    public ProblemStarterCode() {}

    public ProblemStarterCode(Problem problem, CodeSubmission.Language language, String code) {
        this.problem = problem;
        this.language = language;
        this.code = code;
    }

    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem p) { this.problem = p; }
    public CodeSubmission.Language getLanguage() { return language; }
    public void setLanguage(CodeSubmission.Language l) { this.language = l; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
}
