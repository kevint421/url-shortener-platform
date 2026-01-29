package com.urlshortener.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS configuration for the API Gateway.
 *
 * This CorsWebFilter provides a single, unified CORS configuration for ALL endpoints:
 * - Local endpoints (e.g., /api/auth/** handled by AuthController)
 * - Proxied endpoints (e.g., /api/urls/** routed to URL Service)
 * - Public endpoints (e.g., /{shortCode} redirects)
 *
 * Running with HIGHEST_PRECEDENCE ensures CORS headers are added before any other processing,
 * including Spring Security and Gateway routing filters.
 *
 * Configuration:
 * - Allows all origin patterns (suitable for local development)
 * - Allows all HTTP methods including OPTIONS (CORS preflight)
 * - Allows credentials (cookies, authorization headers)
 * - 1-hour preflight cache
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setExposedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
