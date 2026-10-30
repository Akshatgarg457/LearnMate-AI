package com.learnmate.backend.dashboard.service;

import com.learnmate.backend.dashboard.dto.DashboardSummary;
import com.learnmate.backend.exam.entity.MockExamAttempt;
import com.learnmate.backend.exam.entity.QuizAttempt;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.progress.entity.UserProgress;
import com.learnmate.backend.progress.repository.LearningActivityRepository;
import com.learnmate.backend.progress.repository.UserProgressRepository;
import com.learnmate.backend.reward.repository.RewardTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class DashboardService {

    private final UserProgressRepository progressRepo;
    private final RewardTransactionRepository rewardRepo;
    private final CodeSubmissionRepository submissionRepo;
    private final QuizAttemptRepository quizAttemptRepo;
    private final MockExamAttemptRepository mockExamAttemptRepo;
    private final LearningActivityRepository activityRepo;

    public DashboardService(UserProgressRepository progressRepo,
                             RewardTransactionRepository rewardRepo,
                             CodeSubmissionRepository submissionRepo,
                             QuizAttemptRepository quizAttemptRepo,
                             MockExamAttemptRepository mockExamAttemptRepo,
                             LearningActivityRepository activityRepo) {
        this.progressRepo = progressRepo;
        this.rewardRepo = rewardRepo;
        this.submissionRepo = submissionRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.mockExamAttemptRepo = mockExamAttemptRepo;
        this.activityRepo = activityRepo;
    }

    public DashboardSummary getSummary(Long userId) {
        Optional<UserProgress> progressOpt = progressRepo.findById(userId);

        int currentStreak   = progressOpt.map(UserProgress::getCurrentStreak).orElse(0);
        int longestStreak   = progressOpt.map(UserProgress::getLongestStreak).orElse(0);
        int totalXp         = progressOpt.map(UserProgress::getTotalXp).orElse(0);
        int level           = progressOpt.map(UserProgress::getLevel).orElse(1);

        int rewardBalance = rewardRepo.sumBalanceByUserId(userId);

        long totalSubmissions = submissionRepo
                .findByUserIdOrderBySubmittedAtDesc(userId, PageRequest.of(0, 1))
                .getTotalElements();

        long quizzesCompleted = quizAttemptRepo
                .countByUserIdAndStatus(userId, QuizAttempt.Status.SUBMITTED);

        long mockExamsCompleted = mockExamAttemptRepo
                .countByUserIdAndStatus(userId, MockExamAttempt.Status.SUBMITTED);

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long activeDays = activityRepo
                .findActiveDatesBetween(userId, thirtyDaysAgo, LocalDate.now())
                .size();

        return new DashboardSummary(
                currentStreak, longestStreak, totalXp, level,
                rewardBalance, totalSubmissions, quizzesCompleted,
                mockExamsCompleted, activeDays
        );
    }
}
