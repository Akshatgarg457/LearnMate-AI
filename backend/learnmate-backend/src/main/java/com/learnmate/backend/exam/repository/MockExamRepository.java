package com.learnmate.backend.exam.repository;
import com.learnmate.backend.exam.entity.MockExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MockExamRepository extends JpaRepository<MockExam, Long> {
    Page<MockExam> findByActiveTrue(Pageable p);
    Page<MockExam> findByActiveTrueAndSubject(String subject, Pageable p);
}
