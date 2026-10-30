package com.learnmate.backend.exam.dto;

import java.util.List;

public record RunResultDto(
        String status,
        int passed,
        int total,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        String compileOutput,
        List<SubmissionCaseDto> cases
) {}
