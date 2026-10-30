package com.learnmate.backend.progress.repository;
import com.learnmate.backend.progress.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {}
