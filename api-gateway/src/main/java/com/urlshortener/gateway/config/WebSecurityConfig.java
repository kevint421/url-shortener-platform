package com.urlshortener.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)  // Disable CSRF for REST API
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/api/auth/**").permitAll()     // Allow auth endpoints
                .pathMatchers("/actuator/**").permitAll()     // Allow health checks
                .pathMatchers("/{shortCode}").permitAll()     // Allow redirects
                .anyExchange().permitAll()                    // Allow all for now (Gateway handles auth)
            );
        
        return http.build();
    }
}