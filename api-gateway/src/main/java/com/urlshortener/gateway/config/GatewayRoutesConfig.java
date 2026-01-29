package com.urlshortener.gateway.config;

import com.urlshortener.gateway.filter.JwtAuthenticationFilter;
import com.urlshortener.gateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${services.url-service.url:http://localhost:8081}")
    private String urlServiceUrl;

    @Value("${services.analytics-service.url:http://localhost:8082}")
    private String analyticsServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth routes (/api/auth/**) are handled by the gateway's own AuthController
                // These are NOT proxied - handled locally by Spring WebFlux reactive controllers
                // CORS for these endpoints is provided by CorsWebFilter (see CorsConfig.java)

                // URL Management routes (JWT required)
                .route("url_management", r -> r
                        .path("/api/urls/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri(urlServiceUrl))

                // Analytics routes (JWT required)
                .route("analytics", r -> r
                        .path("/api/analytics/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri(analyticsServiceUrl))

                // Redirect routes (public, no JWT, but rate limited)
                .route("redirect", r -> r
                        .path("/{shortCode}")
                        .filters(f -> f
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri(urlServiceUrl))

                .build();
    }
}