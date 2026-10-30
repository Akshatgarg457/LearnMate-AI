package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_options")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuizOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private QuizQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_correct", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean correct = false;

    @Column(name = "option_index", nullable = false)
    private int optionIndex = 0;

    public Long getId() { return id; }
    public QuizQuestion getQuestion() { return question; }
    public void setQuestion(QuizQuestion q) { this.question = q; }
    public String getText() { return text; }
    public void setText(String t) { this.text = t; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean c) { this.correct = c; }
    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int o) { this.optionIndex = o; }
}
