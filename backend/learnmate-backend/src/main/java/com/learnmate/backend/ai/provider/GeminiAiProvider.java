package com.learnmate.backend.ai.provider;

import com.learnmate.backend.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini adapter (Generative Language API).
 * Active when AI_PROVIDER=gemini and a real AI_API_KEY is configured.
 */
@Component
@ConditionalOnExpression("'${app.ai.provider:anthropic}'.equalsIgnoreCase('gemini') "
        + "&& !'${app.ai.api-key:NOT_CONFIGURED}'.equals('NOT_CONFIGURED')")
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String SYSTEM_PROMPT =
            "You are LearnMate AI, a helpful study assistant for students. " +
            "Explain concepts clearly, give examples, and encourage deeper understanding.";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiAiProvider(
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.max-tokens}") int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String chat(List<Message> history, String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Message m : history)
            contents.add(turn(m.role(), m.content()));
        contents.add(turn("user", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        body.put("contents", contents);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        // 2.5 Flash spends maxOutputTokens on hidden reasoning by default, truncating short answers.
        if (model.startsWith("gemini-2.5-flash"))
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        try {
            return request(body, headers);
        } catch (ApiException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status != 429 && status < 500) {
                log.warn("Gemini returned HTTP {}: {}", status, e.getResponseBodyAsString());
                throw ApiException.serviceUnavailable("AI provider request failed (" + status + ")");
            }
            log.warn("Gemini returned HTTP {}; retrying once", status);
            sleepQuietly();
            try {
                return request(body, headers);
            } catch (HttpStatusCodeException retry) {
                log.warn("Gemini retry returned HTTP {}: {}", retry.getStatusCode().value(), retry.getResponseBodyAsString());
                throw ApiException.serviceUnavailable("AI provider request failed (" + retry.getStatusCode().value() + ")");
            } catch (Exception retry) {
                log.warn("Gemini retry failed: {}", retry.toString());
                throw ApiException.serviceUnavailable("AI provider request failed");
            }
        } catch (Exception e) {
            log.warn("Gemini request failed: {}", e.toString());
            throw ApiException.serviceUnavailable("AI provider request failed");
        }
    }

    @SuppressWarnings("unchecked")
    private String request(Map<String, Object> body, HttpHeaders headers) {
        var resp = restTemplate.exchange(
                BASE_URL + model + ":generateContent",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        var candidates = (List<Map<String, Object>>) resp.getBody().get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            if (parts != null && !parts.isEmpty())
                return (String) parts.get(0).get("text");
        }
        throw ApiException.serviceUnavailable("AI provider returned empty response");
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Map<String, Object> turn(String role, String text) {
        String geminiRole = "assistant".equalsIgnoreCase(role) ? "model" : "user";
        return Map.of("role", geminiRole, "parts", List.of(Map.of("text", text)));
    }
}
