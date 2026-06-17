package com.portfolio.backend.config;

import com.portfolio.backend.modulo_portfolio.domain.exception.IdempotencyConflictException;
import com.portfolio.backend.modulo_portfolio.domain.exception.IdempotencyKeyMissingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotency:";
    private final StringRedisTemplate redisTemplate;

    public IdempotencyInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().contains("/api/v1/admin/portfolio")) {
            String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
            if (key == null || key.isBlank()) {
                throw new IdempotencyKeyMissingException("O cabeçalho 'Idempotency-Key' é obrigatório para esta operação.");
            }

            String redisKey = REDIS_PREFIX + key;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(10));
            if (success == null || !success) {
                throw new IdempotencyConflictException("Esta requisição já foi processada ou está em processamento.");
            }
            request.setAttribute("validIdempotencyKey", redisKey);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String redisKey = (String) request.getAttribute("validIdempotencyKey");
        if (redisKey != null) {
            int status = response.getStatus();
            if (status >= 200 && status < 300 && ex == null) {
                redisTemplate.opsForValue().set(redisKey, "COMPLETED", Duration.ofHours(24));
            } else {
                redisTemplate.delete(redisKey);
            }
        }
    }
}
