package com.learnmate.backend.ai.provider;

import com.learnmate.backend.common.exception.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnExpression("'${app.ai.api-key:NOT_CONFIGURED}'.equals('NOT_CONFIGURED') "
        + "|| (!'${app.ai.provider:anthropic}'.equalsIgnoreCase('anthropic') "
        + "&& !'${app.ai.provider:anthropic}'.equalsIgnoreCase('gemini'))")
public class UnconfiguredAiProvider implements AiProvider {
    @Override
    public String chat(List<Message> history, String userMessage) {
        throw ApiException.serviceUnavailable(
                "AI Tutor is not available. Configure AI_API_KEY.");
    }
}
