package com.learnmate.backend.community.repository;
import com.learnmate.backend.community.entity.CommunityQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, Long> {
    Page<CommunityQuestion> findByStatusOrderByCreatedAtDesc(CommunityQuestion.Status status, Pageable p);
    Page<CommunityQuestion> findByStatusAndSubjectOrderByCreatedAtDesc(CommunityQuestion.Status status, String subject, Pageable p);
    @Query("SELECT q FROM CommunityQuestion q WHERE q.status = 'OPEN' AND (LOWER(q.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(q.body) LIKE LOWER(CONCAT('%',:kw,'%')))")
    Page<CommunityQuestion> search(String kw, Pageable p);
}
