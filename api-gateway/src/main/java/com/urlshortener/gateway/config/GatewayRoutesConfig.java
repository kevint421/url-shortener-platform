package com.urlshortener.gateway.config;

import com.urlshortener.gateway.filter.JwtAuthenticationFilter;
import com.urlshortener.gateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth routes (no JWT required)
                .route("auth_routes", r -> r
                        .path("/api/auth/**")
                        .uri("http://url-shortener-gateway:8080"))
                
                // URL Management routes (JWT required)
                .route("url_management", r -> r
                        .path("/api/urls/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("http://url-service:8081"))
                
                // Analytics routes (JWT required) - for future
                .route("analytics", r -> r
                        .path("/api/analytics/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("http://analytics-service:8082"))
                
                // Redirect routes (public, no JWT, but rate limited)
                .route("redirect", r -> r
                        .path("/{shortCode}")
                        .filters(f -> f
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("http://url-service:8081"))
                
                .build();
    }
}