package com.urlshortener.url.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for URL Service.
 *
 * CORS Configuration:
 * - CORS is handled by the API Gateway (single entry point)
 * - Backend services should NOT configure CORS to avoid duplicate headers
 * - All requests to this service go through the Gateway, which adds CORS headers
 *
 * Direct Access:
 * - If this service needs to be accessed directly (without Gateway), uncomment CORS configuration below
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS disabled - handled by API Gateway
    // Uncomment if service needs direct access (bypassing Gateway)
    /*
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(true)
                .maxAge(3600);
    }
    */
}
