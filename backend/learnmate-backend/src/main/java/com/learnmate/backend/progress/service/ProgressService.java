package com.learnmate.backend.progress.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.progress.entity.*;
import com.learnmate.backend.progress.repository.*;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ProgressService {

    private static final int XP_PER_LEVEL = 100;
    private static final int[] STREAK_MILESTONES = {7, 14, 30, 60, 100, 365};
    private static final int STREAK_MILESTONE_POINTS = 50;

    private final LearningActivityRepository activityRepo;
    private final UserProgressRepository progressRepo;
    private final AchievementRepository achievementRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final UserRepository userRepo;
    private final RewardService rewardService;

    public ProgressService(LearningActivityRepository activityRepo,
                           UserProgressRepository progressRepo,
                           AchievementRepository achievementRepo,
                           UserAchievementRepository userAchievementRepo,
                           UserRepository userRepo,
                           @Lazy RewardService rewardService) {
        this.activityRepo = activityRepo;
        this.progressRepo = progressRepo;
        this.achievementRepo = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.userRepo = userRepo;
        this.rewardService = rewardService;
    }

    /**
     * Called by other services when a qualifying activity occurs.
     * Idempotent per (userId, date, activityType).
     */
    @Transactional
    public void recordActivity(Long userId, LearningActivity.ActivityType type, Long referenceId, int xp) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        LocalDate today = LocalDate.now(ZoneId.of(tz));

        // Prevent duplicate activity record for same type on same day
        if (activityRepo.existsByUserIdAndActivityDateAndActivityType(userId, today, type)) return;

        LearningActivity activity = new LearningActivity();
        activity.setUser(user);
        activity.setActivityType(type);
        activity.setActivityDate(today);
        activity.setXpAwarded(xp);
        activity.setReferenceId(referenceId);
        activityRepo.save(activity);

        updateProgress(user, today, xp, tz);
    }

    @Transactional
    public void updateProgress(User user, LocalDate today, int xp, String tz) {
        UserProgress progress = progressRepo.findById(user.getId()).orElseGet(() -> {
            UserProgress p = new UserProgress(); p.setUser(user); return p;
        });

        // Streak logic
        LocalDate last = progress.getLastActivityDate();
        if (last == null) {
            progress.setCurrentStreak(1);
        } else if (last.equals(today)) {
            // Already updated today — just add XP
        } else if (last.equals(today.minusDays(1))) {
            progress.setCurrentStreak(progress.getCurrentStreak() + 1);
        } else {
            progress.setCurrentStreak(1); // streak broken
        }
        if (progress.getCurrentStreak() > progress.getLongestStreak()) {
            progress.setLongestStreak(progress.getCurrentStreak());
        }
        progress.setLastActivityDate(today);

        // XP and level
        int newXp = progress.getTotalXp() + xp;
        progress.setTotalXp(newXp);
        int level = 1 + newXp / XP_PER_LEVEL;
        progress.setLevel(level);
        progress.setNextLevelXp((level * XP_PER_LEVEL) - newXp);

        progressRepo.save(progress);

        // Check streak milestone rewards
        int streak = progress.getCurrentStreak();
        for (int milestone : STREAK_MILESTONES) {
            if (streak == milestone) {
                String idem = "streak-milestone-" + user.getId() + "-" + milestone;
                rewardService.credit(user.getId(), STREAK_MILESTONE_POINTS,
                        RewardTransaction.TransactionType.STREAK_REWARD,
                        "Streak milestone: " + milestone + " days", idem, null);
            }
        }
    }

    public UserProgress getProgress(Long userId) {
        return progressRepo.findById(userId).orElseGet(() -> {
            User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
            UserProgress p = new UserProgress(); p.setUser(user);
            return progressRepo.save(p);
        });
    }

    public List<Map<String, Object>> getHeatmap(Long userId, LocalDate from, LocalDate to) {
        List<Object[]> raw = activityRepo.findDailyXpBetween(userId, from, to);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", row[0].toString());
            entry.put("xp", ((Number) row[1]).intValue());
            result.add(entry);
        }
        return result;
    }

    public List<UserAchievement> getAchievements(Long userId) {
        return userAchievementRepo.findByUserIdOrderByEarnedAtDesc(userId);
    }

    public Page<LearningActivity> getActivityHistory(Long userId, Pageable pageable) {
        return activityRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
