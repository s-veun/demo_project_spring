package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProfileUploadRateLimiter {

    private final Map<String, Deque<Long>> userUploadWindow = new ConcurrentHashMap<>();

    @Value("${app.profile-image.rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${app.profile-image.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public void validateRequest(String principal) {
        long nowEpoch = Instant.now().getEpochSecond();
        Deque<Long> queue = userUploadWindow.computeIfAbsent(principal, key -> new ArrayDeque<>());

        synchronized (queue) {
            while (!queue.isEmpty() && nowEpoch - queue.peekFirst() >= windowSeconds) {
                queue.pollFirst();
            }

            if (queue.size() >= maxRequests) {
                throw new TooManyRequestsException("Upload rate limit exceeded. Please try again later.");
            }

            queue.addLast(nowEpoch);
        }
    }
}

