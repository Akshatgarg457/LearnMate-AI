package com.learnmate.backend.exam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuizQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stem;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(nullable = false)
    private int marks = 1;

    @Column(columnDefinition = "TEXT")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionIndex ASC")
    private List<QuizOption> options = new ArrayList<>();

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz q) { this.quiz = q; }
    public String getStem() { return stem; }
    public void setStem(String s) { this.stem = s; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int o) { this.orderIndex = o; }
    public int getMarks() { return marks; }
    public void setMarks(int m) { this.marks = m; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }
    public List<QuizOption> getOptions() { return options; }
}
