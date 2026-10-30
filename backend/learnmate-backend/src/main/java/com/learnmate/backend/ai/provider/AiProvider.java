package com.learnmate.backend.ai.provider;

import java.util.List;

/**
 * Abstraction over the AI provider. Never exposes API keys to callers.
 */
public interface AiProvider {
    /**
     * @param history list of {role, content} maps representing conversation history
     * @param userMessage the new user message
     * @return the assistant's reply
     */
    String chat(List<Message> history, String userMessage);

    record Message(String role, String content) {}
}
