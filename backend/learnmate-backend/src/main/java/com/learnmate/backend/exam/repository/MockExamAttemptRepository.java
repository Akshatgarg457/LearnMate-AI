package com.learnmate.backend.exam.repository;
import com.learnmate.backend.exam.entity.MockExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface MockExamAttemptRepository extends JpaRepository<MockExamAttempt, Long> {
    Page<MockExamAttempt> findByUserIdOrderByStartedAtDesc(Long userId, Pageable p);
    Optional<MockExamAttempt> findByIdAndUserId(Long id, Long userId);
    List<MockExamAttempt> findByUserIdAndExamIdAndStatus(Long userId, Long examId, MockExamAttempt.Status status);
    long countByUserIdAndStatus(Long userId, MockExamAttempt.Status status);
}
