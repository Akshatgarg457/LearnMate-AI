package com.learnmate.backend.ai.repository;
import com.learnmate.backend.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query(value = """
            SELECT m.conversation_id, m.content, m.role
            FROM ai_messages m
            JOIN (
                SELECT conversation_id, MAX(id) AS max_id
                FROM ai_messages
                WHERE conversation_id IN (:ids)
                GROUP BY conversation_id
            ) latest ON latest.max_id = m.id
            """, nativeQuery = true)
    List<Object[]> findLatestByConversationIds(@Param("ids") Collection<Long> ids);
}
