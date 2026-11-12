package com.kitchenflow.kds.edge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kitchenflow.kds.common.security.ApiActor;
import com.kitchenflow.kds.edge.error.ApiExceptionHandler.ErrorBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApplicationRateLimitFilter extends OncePerRequestFilter {
    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000).expireAfterAccess(Duration.ofMinutes(15)).build();
    private final ObjectMapper mapper;
    private final Clock clock;

    public ApplicationRateLimitFilter(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateClass rateClass = classify(request);
        if (rateClass == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = subject(request) + ":" + rateClass.name();
        TokenBucket bucket = buckets.get(key, ignored -> new TokenBucket(rateClass.limit(), clock.millis()));
        Consumption consumption = bucket.consume(rateClass.limit(), rateClass.period().toMillis(), clock.millis());
        response.setHeader("X-RateLimit-Limit", Integer.toString(rateClass.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(consumption.remaining()));
        if (!consumption.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Integer.toString(consumption.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Object correlation = request.getAttribute("correlationId");
            mapper.writeValue(response.getOutputStream(), new ErrorBody(clock.instant(), 429, "RATE_LIMITED",
                    "Request rate limit reached.", request.getRequestURI(), correlation == null ? "" : correlation.toString(),
                    Map.of("rateClass", rateClass.name(), "retryAfterSeconds", consumption.retryAfterSeconds()), List.of()));
            return;
        }
        chain.doFilter(request, response);
    }

    private String subject(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ApiActor actor) {
            return "user:" + actor.id();
        }
        String address = request.getHeader("X-Real-IP");
        if (address == null || address.isBlank()) address = request.getRemoteAddr();
        return "address:" + address;
    }

    private RateClass classify(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if (path.equals("/api/v1/auth/login") && method.equals("POST")) return RateClass.LOGIN;
        if ((path.equals("/api/v1/auth/refresh") || path.equals("/api/v1/auth/logout"))) return RateClass.REFRESH;
        if (path.startsWith("/api/v1/stream/")) return RateClass.SSE_CONNECT;
        if (path.equals("/api/v1/board")) return RateClass.BOARD_READ;
        if (path.startsWith("/api/v1/reports/") || path.equals("/api/v1/audit")) return RateClass.REPORT;
        if (path.startsWith("/api/v1/admin/")) return method.equals("GET") ? RateClass.READ : RateClass.ADMIN_WRITE;
        if (path.startsWith("/api/v1/tickets/") && !method.equals("GET")) return RateClass.TICKET_ACTION;
        if (path.startsWith("/api/v1/orders/") || path.equals("/api/v1/orders")) {
            return method.equals("GET") ? RateClass.READ : RateClass.ORDER_WRITE;
        }
        if (method.equals("GET")) return RateClass.READ;
        return null;
    }

    private enum RateClass {
        LOGIN(5, Duration.ofMinutes(10)), REFRESH(10, Duration.ofMinutes(10)),
        ORDER_WRITE(30, Duration.ofMinutes(1)), TICKET_ACTION(120, Duration.ofMinutes(1)),
        READ(300, Duration.ofMinutes(1)), REPORT(10, Duration.ofMinutes(1)),
        ADMIN_WRITE(60, Duration.ofMinutes(1)), BOARD_READ(60, Duration.ofMinutes(1)),
        SSE_CONNECT(10, Duration.ofMinutes(1));

        private final int limit;
        private final Duration period;

        RateClass(int limit, Duration period) {
            this.limit = limit;
            this.period = period;
        }

        int limit() { return limit; }
        Duration period() { return period; }
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillMillis;

        TokenBucket(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillMillis = now;
        }

        synchronized Consumption consume(int capacity, long periodMillis, long now) {
            if (now > lastRefillMillis) {
                double tokensPerMillisecond = (double) capacity / periodMillis;
                tokens = Math.min(capacity, tokens + (now - lastRefillMillis) * tokensPerMillisecond);
                lastRefillMillis = now;
            }
            if (tokens >= 1) {
                tokens -= 1;
                return new Consumption(true, (int) Math.floor(tokens), 0);
            }
            long millisPerToken = Math.max(1L, periodMillis / capacity);
            long waitMillis = Math.max(1L, millisPerToken - (now - lastRefillMillis));
            return new Consumption(false, 0, (int) Math.max(1L, (waitMillis + 999) / 1000));
        }
    }

    private record Consumption(boolean allowed, int remaining, int retryAfterSeconds) {}
}
