package com.learnmate.backend.ai.repository;
import com.learnmate.backend.ai.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable p);
    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
