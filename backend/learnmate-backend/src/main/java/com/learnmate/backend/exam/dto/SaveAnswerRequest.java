package com.learnmate.backend.exam.dto;

import jakarta.validation.constraints.NotNull;

public record SaveAnswerRequest(
        @NotNull(message = "Question ID is required")
        Long questionId,
        Long optionId  // nullable = clear answer
) {}
