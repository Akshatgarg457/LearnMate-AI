package com.learnmate.backend.exam.judge;

import com.learnmate.backend.common.exception.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Fallback: active when no judge URL is configured. Throws SERVICE_UNAVAILABLE
 * on every call — never executes code locally.
 */
@Component
@ConditionalOnExpression("'${app.judge.base-url:NOT_CONFIGURED}'.equals('NOT_CONFIGURED')")
public class UnconfiguredJudgeProvider implements JudgeProvider {

    @Override
    public JudgeResult execute(String language, String code, String stdin, int timeLimitMs, int memoryLimitMb) {
        throw ApiException.serviceUnavailable(
                "Code execution is not available. Configure JUDGE_BASE_URL (and JUDGE_API_KEY if your judge requires one).");
    }
}
