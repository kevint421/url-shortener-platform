package com.urlshortener.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Minimal Spring Security configuration for the API Gateway.
 *
 * Purpose:
 * - Provides PasswordEncoder bean for AuthService
 * - Disables all Spring Security authorization (Gateway filters handle authentication)
 * - Disables CSRF (stateless JWT API)
 *
 * Authentication Strategy:
 * - Auth endpoints (/api/auth/**): Public access, no JWT required
 * - Protected endpoints (/api/urls/**, /api/analytics/**): JWT validation by Gateway's JwtAuthenticationFilter
 * - Redirect endpoints (/{shortCode}): Public access, rate limited
 *
 * CORS is handled by CorsWebFilter bean (see CorsConfig.java)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll() // Permit all - Gateway filters handle authentication
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
