package com.learnmate.backend.exam.dto;

public record StartQuizRequest(
        boolean untimed  // default false = timed
) {}
