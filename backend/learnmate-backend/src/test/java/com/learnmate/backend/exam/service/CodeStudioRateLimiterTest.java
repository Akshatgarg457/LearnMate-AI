package com.learnmate.backend.exam.service;

import com.learnmate.backend.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class CodeStudioRateLimiterTest {

    @Test
    void blocksAfterThePerMinuteAllowance() {
        CodeStudioRateLimiter limiter = new CodeStudioRateLimiter(2, 100);

        limiter.check(1L);
        limiter.check(1L);

        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void blocksAfterTheHourlyAllowance() {
        CodeStudioRateLimiter limiter = new CodeStudioRateLimiter(100, 3);

        limiter.check(2L);
        limiter.check(2L);
        limiter.check(2L);

        assertThatThrownBy(() -> limiter.check(2L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void limitsArePerUser() {
        CodeStudioRateLimiter limiter = new CodeStudioRateLimiter(1, 1);

        limiter.check(3L);

        assertThatThrownBy(() -> limiter.check(3L)).isInstanceOf(ApiException.class);
        assertThatCode(() -> limiter.check(4L)).doesNotThrowAnyException();
    }
}
