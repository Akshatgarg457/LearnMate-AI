package com.learnmate.backend.exam.repository;
import com.learnmate.backend.exam.entity.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Page<QuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId, Pageable p);
    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);
    List<QuizAttempt> findByUserIdAndQuizIdAndStatus(Long userId, Long quizId, QuizAttempt.Status status);
    long countByUserIdAndStatus(Long userId, QuizAttempt.Status status);
}
