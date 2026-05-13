package com.library.api.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Rate limiting filter using the Token Bucket algorithm via Bucket4j
//
// How Token Bucket works:
// - Each IP address gets a bucket that holds a fixed number of tokens
// - Each incoming request consumes one token from the bucket
// - The bucket refills gradually over time (60 tokens per minute here)
// - When the bucket is empty the request is rejected with HTTP 429 Too Many Requests
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Each IP address gets its own bucket stored in a thread-safe map
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // Maximum allowed requests per minute per IP address
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // Retrieve the existing bucket for this IP, or create a new one
        Bucket bucket = ipBuckets.computeIfAbsent(clientIp, this::createNewBucket);

        // Try to consume one token — succeeds if the bucket is not empty
        if (bucket.tryConsume(1)) {
            // Token consumed successfully — let the request through
            filterChain.doFilter(request, response);
        } else {
            // Bucket is empty — reject with 429
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests. Limit: " + MAX_REQUESTS_PER_MINUTE
                    + " requests per minute. Please wait before retrying.\"}"
            );
        }
    }

    // Create a new bucket for a new IP address
    // Starts full and refills at the same rate it is capped at
    private Bucket createNewBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // Extract the real client IP even when the app is behind a proxy or load balancer
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For may contain multiple IPs — the first one is the original client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
