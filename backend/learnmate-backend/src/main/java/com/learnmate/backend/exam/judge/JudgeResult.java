package com.learnmate.backend.exam.judge;

public record JudgeResult(
        String status,      // "Accepted", "Wrong Answer", "Time Limit Exceeded", etc.
        String stdout,
        String stderr,
        String compileOutput,
        Integer timeMs,
        Integer memoryKb,
        boolean finished    // false = still processing
) {}
