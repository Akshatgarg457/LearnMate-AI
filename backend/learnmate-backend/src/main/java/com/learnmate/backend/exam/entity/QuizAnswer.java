package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_qa_attempt_question", columnNames = {"attempt_id", "question_id"})
})
public class QuizAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuizOption selectedOption;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "marks_awarded")
    private Integer marksAwarded;

    public Long getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public void setAttempt(QuizAttempt a) { this.attempt = a; }
    public QuizQuestion getQuestion() { return question; }
    public void setQuestion(QuizQuestion q) { this.question = q; }
    public QuizOption getSelectedOption() { return selectedOption; }
    public void setSelectedOption(QuizOption o) { this.selectedOption = o; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean c) { this.correct = c; }
    public Integer getMarksAwarded() { return marksAwarded; }
    public void setMarksAwarded(Integer m) { this.marksAwarded = m; }
}
