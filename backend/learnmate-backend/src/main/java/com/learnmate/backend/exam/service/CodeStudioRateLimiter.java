package com.learnmate.backend.exam.service;

import com.learnmate.backend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter for run/submit requests, per authenticated user.
 * The application runs as a single instance; a shared store is required if it scales out.
 */
@Component
public class CodeStudioRateLimiter {

    private final int perMinute;
    private final int perHour;
    private final Map<Long, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public CodeStudioRateLimiter(
            @Value("${app.code-studio.rate-limit-per-minute:10}") int perMinute,
            @Value("${app.code-studio.rate-limit-per-hour:60}") int perHour) {
        this.perMinute = perMinute;
        this.perHour = perHour;
    }

    public void check(Long userId) {
        Instant now = Instant.now();
        Deque<Instant> queue = calls.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (queue) {
            Instant hourAgo = now.minus(Duration.ofHours(1));
            while (!queue.isEmpty() && queue.peekFirst().isBefore(hourAgo)) queue.removeFirst();

            long lastMinute = queue.stream().filter(t -> t.isAfter(now.minus(Duration.ofMinutes(1)))).count();
            if (lastMinute >= perMinute || queue.size() >= perHour) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "You are submitting code too quickly. Wait a moment and try again.");
            }
            queue.addLast(now);
        }
    }
}
