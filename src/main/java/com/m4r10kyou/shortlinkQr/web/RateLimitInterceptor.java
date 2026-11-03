package com.m4r10kyou.shortlinkQr.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.m4r10kyou.shortlinkQr.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int limit;
    private final Cache<String, AtomicInteger> cache;
    private final long windowSeconds;

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /*
     * IMPORTANT: The cache expiration (expireAfterWrite) IS the rate limit window.
     * Caffeine's cache.get() only writes when the key does not exist, and the subsequent
     * incrementAndGet() does not renew the entry's time. Therefore, the key expires exactly
     * `windowSeconds` after the FIRST request.
     * If `expireAfterWrite` is modified in the future to optimize memory,
     * it will unknowingly alter the rate limit behavior.
     */

    public RateLimitInterceptor(
            @Value("${shortlink.rate-limit.requests}") int limit,
            @Value("${shortlink.rate-limit.window-seconds}") long windowSeconds,
            @Value("${shortlink.rate-limit.max-tracked-ips}") long maxTrackedIps
    ) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxTrackedIps)
                .expireAfterWrite(Duration.ofSeconds(windowSeconds))
                .build();
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {

        // 1. Fast and cheap scope check.
        // We only limit link creation.
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().equals("/api/links")) {
            return true;
        }

        // 2. Extract the IP, delegating proxy resolution to Spring (server.forward-headers-strategy)
        String clientIp = request.getRemoteAddr();

        AtomicInteger requestCount = cache.get(clientIp, key -> new AtomicInteger(0));

        String forwarderFor = request.getHeader("X-Forwarded-For");
        String connectingIp = request.getHeader("CF-Connecting-IP");
        String remoteAddr = request.getRemoteAddr();

        log.warn("X-Forwarded-For ->'{}' CF-Connecting-IP -> '{}' RemoteAddr -> '{}'",forwarderFor,connectingIp,remoteAddr);

        if (requestCount.incrementAndGet() > limit) {

            log.warn("Rejected ip {} for exceeding the rate limit", clientIp);
            // 3. Throw exception so the @ControllerAdvice generates the ProblemDetail (RFC 7807)
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.", windowSeconds);
        }

        return true;

    }
}
