package com.learnmate.backend.exam.repository;
import com.learnmate.backend.exam.entity.MockExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface MockExamAnswerRepository extends JpaRepository<MockExamAnswer, Long> {
    List<MockExamAnswer> findByAttemptId(Long attemptId);
    Optional<MockExamAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
