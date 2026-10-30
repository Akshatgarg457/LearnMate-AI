package com.learnmate.backend.progress.repository;
import com.learnmate.backend.progress.entity.LearningActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
    Page<LearningActivity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable p);
    boolean existsByUserIdAndActivityDateAndActivityType(Long userId, LocalDate date, LearningActivity.ActivityType type);
    boolean existsByUserIdAndActivityDate(Long userId, LocalDate date);
    @Query("SELECT DISTINCT a.activityDate FROM LearningActivity a WHERE a.user.id = :userId AND a.activityDate BETWEEN :from AND :to ORDER BY a.activityDate")
    List<LocalDate> findActiveDatesBetween(Long userId, LocalDate from, LocalDate to);
    @Query("SELECT a.activityDate, SUM(a.xpAwarded) FROM LearningActivity a WHERE a.user.id = :userId AND a.activityDate BETWEEN :from AND :to GROUP BY a.activityDate")
    List<Object[]> findDailyXpBetween(Long userId, LocalDate from, LocalDate to);
    long countByUserIdAndActivityType(Long userId, LearningActivity.ActivityType type);
}
