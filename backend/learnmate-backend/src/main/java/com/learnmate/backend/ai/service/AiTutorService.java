package com.learnmate.backend.ai.service;

import com.learnmate.backend.ai.dto.AiConversationSummary;
import com.learnmate.backend.ai.entity.*;
import com.learnmate.backend.ai.provider.AiProvider;
import com.learnmate.backend.ai.repository.*;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTutorService {

    private final AiConversationRepository convRepo;
    private final AiMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final AiProvider aiProvider;
    private final ProgressService progressService;

    public AiTutorService(AiConversationRepository convRepo, AiMessageRepository messageRepo,
                          UserRepository userRepo, AiProvider aiProvider, ProgressService progressService) {
        this.convRepo = convRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.aiProvider = aiProvider;
        this.progressService = progressService;
    }

    @Transactional
    public AiConversation createConversation(Long userId, String title, String subject) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        AiConversation conv = new AiConversation();
        conv.setUser(user);
        conv.setTitle(title != null ? title : "New conversation");
        conv.setSubject(subject);
        return convRepo.save(conv);
    }

    @Transactional
    public AiMessage sendMessage(Long userId, Long conversationId, String userContent) {
        if (userContent == null || userContent.isBlank())
            throw ApiException.badRequest("Message content cannot be empty");
        if (userContent.length() > 4000)
            throw ApiException.badRequest("Message too long (max 4000 characters)");

        AiConversation conv = convRepo.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));

        // Persist user message
        AiMessage userMsg = new AiMessage();
        userMsg.setConversation(conv);
        userMsg.setRole(AiMessage.Role.USER);
        userMsg.setContent(userContent);
        messageRepo.save(userMsg);

        // Build history for provider
        List<AiMessage> history = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<AiProvider.Message> providerHistory = history.stream()
                .filter(m -> m.getId() != null && !m.getId().equals(userMsg.getId()))
                .map(m -> new AiProvider.Message(m.getRole().name().toLowerCase(), m.getContent()))
                .toList();

        // Call provider — throws SERVICE_UNAVAILABLE if not configured
        String reply = aiProvider.chat(providerHistory, userContent);

        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setConversation(conv);
        assistantMsg.setRole(AiMessage.Role.ASSISTANT);
        assistantMsg.setContent(reply);
        assistantMsg = messageRepo.save(assistantMsg);

        progressService.recordActivity(userId, LearningActivity.ActivityType.AI_CONVERSATION, conv.getId(), 5);
        return assistantMsg;
    }

    public Page<AiConversationSummary> listConversations(Long userId, Pageable pageable) {
        Page<AiConversation> page = convRepo.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        List<Long> ids = page.getContent().stream().map(AiConversation::getId).toList();
        Map<Long, Object[]> latest = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : messageRepo.findLatestByConversationIds(ids))
                latest.put(((Number) row[0]).longValue(), row);
        }
        return page.map(c -> {
            Object[] last = latest.get(c.getId());
            return new AiConversationSummary(
                    c.getId(), c.getTitle(), c.getSubject(), c.getCreatedAt(), c.getUpdatedAt(),
                    last != null ? preview((String) last[1]) : null,
                    last != null ? (String) last[2] : null);
        });
    }

    private static String preview(String content) {
        if (content == null) return null;
        String oneLine = content
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s+", "")
                .replaceAll("[*_#>]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return oneLine.length() <= 160 ? oneLine : oneLine.substring(0, 160) + "...";
    }

    public List<AiMessage> getMessages(Long userId, Long conversationId) {
        convRepo.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
