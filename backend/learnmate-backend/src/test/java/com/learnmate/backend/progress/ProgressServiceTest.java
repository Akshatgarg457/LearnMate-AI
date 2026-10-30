package com.learnmate.backend.progress;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.entity.UserProgress;
import com.learnmate.backend.progress.service.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProgressServiceTest {

    @Autowired ProgressService progressService;
    @Autowired AuthService authService;

    private Long userId;

    @BeforeEach
    void setup() {
        userId = authService.register(new RegisterRequest("Prog User", "prog@test.com", "pass12345")).userId();
    }

    @Test
    void recordActivity_incrementsXp() {
        progressService.recordActivity(userId, LearningActivity.ActivityType.QUIZ_COMPLETED, 1L, 15);
        UserProgress p = progressService.getProgress(userId);
        assertThat(p.getTotalXp()).isEqualTo(15);
        assertThat(p.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void recordSameActivitySameDay_idempotent() {
        progressService.recordActivity(userId, LearningActivity.ActivityType.QUIZ_COMPLETED, 1L, 15);
        progressService.recordActivity(userId, LearningActivity.ActivityType.QUIZ_COMPLETED, 2L, 15);
        UserProgress p = progressService.getProgress(userId);
        // Second call is ignored because same type+day
        assertThat(p.getTotalXp()).isEqualTo(15);
    }

    @Test
    void differentActivityTypes_sameDay_bothRecorded() {
        progressService.recordActivity(userId, LearningActivity.ActivityType.QUIZ_COMPLETED, 1L, 15);
        progressService.recordActivity(userId, LearningActivity.ActivityType.CODE_SUBMISSION_ACCEPTED, 2L, 20);
        UserProgress p = progressService.getProgress(userId);
        assertThat(p.getTotalXp()).isEqualTo(35);
    }

    @Test
    void xpLevelCalculation() {
        // Level formula: 1 + totalXp / 100
        progressService.recordActivity(userId, LearningActivity.ActivityType.MOCK_EXAM_COMPLETED, 1L, 120);
        UserProgress p = progressService.getProgress(userId);
        assertThat(p.getLevel()).isEqualTo(2); // 1 + 120/100 = 2
    }
}
