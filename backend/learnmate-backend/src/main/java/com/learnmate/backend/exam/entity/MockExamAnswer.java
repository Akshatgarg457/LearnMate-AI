package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "mock_exam_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mea_attempt_question", columnNames = {"attempt_id", "question_id"})
})
public class MockExamAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private MockExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private MockExamQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private MockExamOption selectedOption;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "marks_awarded")
    private Integer marksAwarded;

    public Long getId() { return id; }
    public MockExamAttempt getAttempt() { return attempt; }
    public void setAttempt(MockExamAttempt a) { this.attempt = a; }
    public MockExamQuestion getQuestion() { return question; }
    public void setQuestion(MockExamQuestion q) { this.question = q; }
    public MockExamOption getSelectedOption() { return selectedOption; }
    public void setSelectedOption(MockExamOption o) { this.selectedOption = o; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean c) { this.correct = c; }
    public Integer getMarksAwarded() { return marksAwarded; }
    public void setMarksAwarded(Integer m) { this.marksAwarded = m; }
}
