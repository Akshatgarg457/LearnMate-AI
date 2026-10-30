package com.learnmate.backend.exam.repository;

import com.learnmate.backend.exam.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {
    Page<Problem> findByActiveTrue(Pageable p);
    Page<Problem> findByActiveTrueAndDifficulty(Problem.Difficulty d, Pageable p);
    Page<Problem> findByActiveTrueAndTopic(String topic, Pageable p);
    Page<Problem> findByActiveTrueAndDifficultyAndTopic(Problem.Difficulty d, String topic, Pageable p);
    Optional<Problem> findBySlug(String slug);
    long countByActiveTrue();

    @Query("select p.topic, count(p) from Problem p where p.active = true group by p.topic")
    List<Object[]> countActiveByTopic();
}
