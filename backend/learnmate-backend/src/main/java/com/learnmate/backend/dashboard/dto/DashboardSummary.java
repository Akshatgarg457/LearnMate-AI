package com.learnmate.backend.dashboard.dto;

public record DashboardSummary(
        int currentStreak,
        int longestStreak,
        int totalXp,
        int level,
        int rewardBalance,
        long totalCodeSubmissions,
        long quizzesCompleted,
        long mockExamsCompleted,
        long activeDaysLast30
) {}
