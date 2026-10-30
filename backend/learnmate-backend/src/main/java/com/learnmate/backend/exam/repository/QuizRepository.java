package com.learnmate.backend.exam.repository;
import com.learnmate.backend.exam.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Page<Quiz> findByActiveTrue(Pageable p);
    Page<Quiz> findByActiveTrueAndSubject(String subject, Pageable p);
}
