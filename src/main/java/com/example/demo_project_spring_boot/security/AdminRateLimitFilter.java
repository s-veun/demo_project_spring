package com.example.demo_project_spring_boot.security;

import com.example.demo_project_spring_boot.exception.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
public class AdminRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();

    @Value("${app.admin.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${app.admin.rate-limit.max-requests:120}")
    private int maxRequests;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/v1/admin/")
                || path.equals("/api/v1/admin/login")
                || path.equals("/api/v1/admin/register");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long now = Instant.now().getEpochSecond();
        long cutoff = now - windowSeconds;

        Deque<Long> bucket = requestBuckets.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst() < cutoff) {
                bucket.pollFirst();
            }

            if (bucket.size() >= maxRequests) {
                log.warn("Admin rate limit exceeded for key={} count={}", key, bucket.size());
                throw new TooManyRequestsException("Too many admin requests. Please try again later.");
            }

            bucket.addLast(now);
        }

        filterChain.doFilter(request, response);
    }
}

