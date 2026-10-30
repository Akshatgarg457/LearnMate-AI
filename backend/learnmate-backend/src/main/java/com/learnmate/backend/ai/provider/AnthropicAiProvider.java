package com.learnmate.backend.ai.provider;

import com.learnmate.backend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Anthropic Messages API adapter.
 * Active when AI_API_KEY is configured to a real key.
 */
@Component
@ConditionalOnExpression("'${app.ai.provider:anthropic}'.equalsIgnoreCase('anthropic') "
        + "&& !'${app.ai.api-key:NOT_CONFIGURED}'.equals('NOT_CONFIGURED')")
public class AnthropicAiProvider implements AiProvider {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM_PROMPT =
            "You are LearnMate AI, a helpful study assistant for students. " +
            "Explain concepts clearly, give examples, and encourage deeper understanding.";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final RestTemplate restTemplate = new RestTemplate();

    public AnthropicAiProvider(
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.max-tokens}") int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(List<Message> history, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (Message m : history)
            messages.add(Map.of("role", m.role(), "content", m.content()));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", SYSTEM_PROMPT);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        try {
            var resp = restTemplate.exchange(
                    API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            var content = (List<Map<String, Object>>) resp.getBody().get("content");
            if (content != null && !content.isEmpty())
                return (String) content.get(0).get("text");
            throw ApiException.serviceUnavailable("AI provider returned empty response");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.serviceUnavailable("AI provider request failed");
        }
    }
}
