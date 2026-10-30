package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    @JsonIgnore
    private Problem problem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    /** Sample test cases are shown to the student; hidden ones are not. */
    @Column(nullable = false)
    private boolean sample = false;

    /** Shown only for sample cases — explains how the expected output is produced. */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem p) { this.problem = p; }
    public String getInput() { return input; }
    public void setInput(String i) { this.input = i; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String e) { this.expectedOutput = e; }
    public boolean isSample() { return sample; }
    public void setSample(boolean s) { this.sample = s; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int o) { this.orderIndex = o; }
}
