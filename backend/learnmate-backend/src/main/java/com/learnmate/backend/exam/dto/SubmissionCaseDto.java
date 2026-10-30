package com.learnmate.backend.exam.dto;

/**
 * Per-case result. Input/expected/actual/stderr are populated for sample cases
 * only — hidden case data never leaves the server.
 */
public record SubmissionCaseDto(
        int index,
        boolean sample,
        boolean passed,
        String status,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        String input,
        String expectedOutput,
        String actualOutput,
        String stderr
) {}
