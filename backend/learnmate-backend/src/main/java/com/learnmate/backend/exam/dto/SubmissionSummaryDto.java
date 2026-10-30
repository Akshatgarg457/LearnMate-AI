package com.learnmate.backend.exam.dto;

import java.time.Instant;

public record SubmissionSummaryDto(
        Long id,
        Long problemId,
        String problemTitle,
        String language,
        String status,
        Integer passedTests,
        Integer totalTests,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        Instant submittedAt,
        Instant judgedAt
) {}
