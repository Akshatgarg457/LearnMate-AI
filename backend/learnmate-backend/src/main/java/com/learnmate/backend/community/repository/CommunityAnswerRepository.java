package com.learnmate.backend.community.repository;
import com.learnmate.backend.community.entity.CommunityAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CommunityAnswerRepository extends JpaRepository<CommunityAnswer, Long> {
    Page<CommunityAnswer> findByQuestionIdAndStatusOrderByVoteCountDesc(Long questionId, CommunityAnswer.Status status, Pageable p);
    Optional<CommunityAnswer> findByIdAndAuthorId(Long id, Long authorId);
    long countByAuthorIdAndStatus(Long authorId, CommunityAnswer.Status status);
}
