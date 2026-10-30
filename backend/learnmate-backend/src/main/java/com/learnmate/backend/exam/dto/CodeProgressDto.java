package com.learnmate.backend.exam.dto;

import java.util.List;

public record CodeProgressDto(
        long totalProblems,
        long attempted,
        long solved,
        DifficultyStats solvedByDifficulty,
        double acceptanceRate,
        long totalSubmissions,
        long acceptedSubmissions,
        int currentStreak,
        int longestStreak,
        int totalXp,
        int level,
        List<TopicProgress> topics,
        List<Long> solvedProblemIds,
        List<SubmissionSummaryDto> recentSubmissions
) {
    public record DifficultyStats(long easy, long medium, long hard) {}

    public record TopicProgress(String topic, long total, long solved) {}
}
