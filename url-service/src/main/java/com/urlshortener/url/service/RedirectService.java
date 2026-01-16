package com.urlshortener.url.service;

import com.urlshortener.common.constants.KafkaTopics;
import com.urlshortener.common.event.EventType;
import com.urlshortener.common.event.UrlAccessedEvent;
import com.urlshortener.common.util.EventIdGenerator;
import com.urlshortener.common.util.UserAgentParser;
import com.urlshortener.url.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {
    
    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Handle redirect - optimized for performance
     * This is the HOT PATH - must be < 50ms
     */
    public String redirect(String shortCode, HttpServletRequest request) {
        // Get long URL (cached or from DB)
        String longUrl = urlService.getLongUrl(shortCode);
        
        // Asynchronously publish click event and update metrics
        // This does NOT block the redirect response
        trackClickAsync(shortCode, request);
        
        return longUrl;
    }
    
    /**
     * Track click asynchronously - does not block redirect
     */
    @Async
    public void trackClickAsync(String shortCode, HttpServletRequest request) {
        try {
            // Extract request metadata
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");
            
            // Parse user agent
            String deviceType = UserAgentParser.getDeviceType(userAgent);
            String browser = UserAgentParser.getBrowser(userAgent);
            String os = UserAgentParser.getOperatingSystem(userAgent);
            
            // Publish access event to Kafka for analytics processing
            publishUrlAccessedEvent(shortCode, ipAddress, userAgent, referer, 
                                   deviceType, browser, os);
            
            // Increment DB click counter (low priority, can be async)
            incrementClickCountAsync(shortCode);
            
        } catch (Exception e) {
            log.error("Failed to track click for {}", shortCode, e);
            // Don't fail the redirect even if tracking fails
        }
    }
    
    /**
     * Publish URL accessed event to Kafka
     */
    private void publishUrlAccessedEvent(String shortCode, String ipAddress, 
                                        String userAgent, String referer,
                                        String deviceType, String browser, String os) {
        try {
            UrlAccessedEvent event = UrlAccessedEvent.builder()
                    .eventId(EventIdGenerator.generate())
                    .timestamp(LocalDateTime.now())
                    .eventType(EventType.URL_ACCESSED.getValue())
                    .shortCode(shortCode)
                    .accessedAt(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referer(referer)
                    .country(null) // Will be enriched by analytics service
                    .city(null)    // Will be enriched by analytics service
                    .deviceType(deviceType)
                    .browser(browser)
                    .operatingSystem(os)
                    .build();
            
            kafkaTemplate.send(KafkaTopics.URL_ACCESS_EVENTS, shortCode, event);
            log.debug("Published URL accessed event: {}", shortCode);
        } catch (Exception e) {
            log.error("Failed to publish URL accessed event", e);
        }
    }
    
    /**
     * Increment click count in database (async, low priority)
     */
    @Async
    public void incrementClickCountAsync(String shortCode) {
        try {
            urlRepository.incrementClickCount(shortCode);
        } catch (Exception e) {
            log.error("Failed to increment click count for {}", shortCode, e);
        }
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}