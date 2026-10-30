package com.learnmate.backend.ai.dto;

import java.time.Instant;

public record AiConversationSummary(
        Long id,
        String title,
        String subject,
        Instant createdAt,
        Instant updatedAt,
        String lastMessage,
        String lastMessageRole
) {}
