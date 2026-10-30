package com.learnmate.backend.progress.repository;
import com.learnmate.backend.progress.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByCode(String code);
    List<Achievement> findByType(Achievement.AchievementType type);
}
