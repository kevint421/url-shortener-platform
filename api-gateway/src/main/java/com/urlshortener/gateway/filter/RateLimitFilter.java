package com.urlshortener.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {
    
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;
    
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    public RateLimitFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Extract user ID from headers (added by JWT filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (userId == null) {
                // For unauthenticated requests, use IP address
                String ipAddress = exchange.getRequest().getRemoteAddress() != null ? 
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
                return checkRateLimit("ip:" + ipAddress, exchange, chain);
            }
            
            return checkRateLimit("user:" + userId, exchange, chain);
        };
    }
    
    /**
     * Check rate limit for a key
     */
    private Mono<Void> checkRateLimit(String key, org.springframework.web.server.ServerWebExchange exchange, 
                                      org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String rateLimitKey = "ratelimit:" + key;
        
        return redisTemplate.opsForValue().increment(rateLimitKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request, set expiration
                        return redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW)
                                .then(chain.filter(exchange));
                    } else if (count > MAX_REQUESTS_PER_MINUTE) {
                        // Rate limit exceeded
                        log.warn("Rate limit exceeded for key: {}", key);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    } else {
                        // Within rate limit
                        return chain.filter(exchange);
                    }
                })
                .onErrorResume(e -> {
                    // If Redis fails, allow the request through
                    log.error("Rate limit check failed: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }
    
    public static class Config {
        // Configuration properties if needed
    }
}
