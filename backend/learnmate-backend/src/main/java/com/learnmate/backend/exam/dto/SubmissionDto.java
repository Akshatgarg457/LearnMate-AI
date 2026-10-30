package com.learnmate.backend.exam.dto;

import java.time.Instant;
import java.util.List;

public record SubmissionDto(
        Long id,
        Long problemId,
        String problemTitle,
        String language,
        String code,
        String status,
        int passedTests,
        int totalTests,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        String errorMessage,
        Instant submittedAt,
        Instant judgedAt,
        List<SubmissionCaseDto> cases
) {}
