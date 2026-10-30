package com.learnmate.backend.progress.repository;
import com.learnmate.backend.progress.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);
    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
    @Query("SELECT COUNT(ua) > 0 FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievement.code = :code")
    boolean existsByUserIdAndAchievementCode(Long userId, String code);
}
