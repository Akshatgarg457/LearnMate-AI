package com.learnmate.backend.exam.repository;

import com.learnmate.backend.exam.entity.Problem;
import com.learnmate.backend.exam.entity.UserProblemProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProblemProgressRepository extends JpaRepository<UserProblemProgress, Long> {

    Optional<UserProblemProgress> findByUserIdAndProblemId(Long userId, Long problemId);

    long countByUserId(Long userId);

    long countByUserIdAndSolvedTrue(Long userId);

    long countByUserIdAndSolvedTrueAndProblemDifficulty(Long userId, Problem.Difficulty difficulty);

    List<UserProblemProgress> findTop10ByUserIdAndSolvedTrueOrderByFirstSolvedAtDesc(Long userId);

    @Query("select up.problem.id from UserProblemProgress up where up.user.id = :userId and up.solved = true")
    List<Long> findSolvedProblemIds(@Param("userId") Long userId);

    @Query("select up.problem.id from UserProblemProgress up where up.user.id = :userId")
    List<Long> findAttemptedProblemIds(@Param("userId") Long userId);

    @Query("select p.topic, count(up) from UserProblemProgress up join up.problem p " +
           "where up.user.id = :userId and up.solved = true group by p.topic")
    List<Object[]> countSolvedByTopic(@Param("userId") Long userId);
}
