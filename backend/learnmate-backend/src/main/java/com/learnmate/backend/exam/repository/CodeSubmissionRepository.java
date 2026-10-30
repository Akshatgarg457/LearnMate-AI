package com.learnmate.backend.exam.repository;

import com.learnmate.backend.exam.entity.CodeSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long>, JpaSpecificationExecutor<CodeSubmission> {
    Page<CodeSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable p);
    Page<CodeSubmission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(Long userId, Long problemId, Pageable p);
    boolean existsByUserIdAndProblemIdAndStatus(Long userId, Long problemId, CodeSubmission.Status status);
    List<CodeSubmission> findByStatusIn(List<CodeSubmission.Status> statuses);

    List<CodeSubmission> findTop5ByUserIdOrderBySubmittedAtDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, CodeSubmission.Status status);

    @Query("select distinct s.problem.id from CodeSubmission s where s.user.id = :userId and s.language = :language")
    List<Long> findProblemIdsByUserAndLanguage(@Param("userId") Long userId,
                                               @Param("language") CodeSubmission.Language language);
}
