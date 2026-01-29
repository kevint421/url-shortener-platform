package com.urlshortener.url.service;

import com.urlshortener.common.constants.KafkaTopics;
import com.urlshortener.common.event.EventType;
import com.urlshortener.common.event.UrlAccessedEvent;
import com.urlshortener.common.util.EventIdGenerator;
import com.urlshortener.common.util.UserAgentParser;
import com.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Separate service for async click tracking to avoid Spring proxy self-invocation issues.
 * When @Async methods are called from the same class, Spring's proxy is bypassed.
 * By moving async logic to a separate service, we ensure proper async execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncClickTrackerService {

    private final UrlRepository urlRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Track click asynchronously - does not block redirect
     * This method is called from RedirectService through Spring proxy, ensuring @Async works
     * @Transactional ensures the DB update is committed (must be on this method, not on incrementClickCount due to self-invocation)
     *
     * @param shortCode The short code of the URL
     * @param ipAddress Client IP address (pre-extracted from request)
     * @param userAgent User-Agent header (pre-extracted from request)
     * @param referer Referer header (pre-extracted from request)
     */
    @Async
    @Transactional
    public void trackClickAsync(String shortCode, String ipAddress, String userAgent, String referer) {
        try {
            // Parse user agent
            String deviceType = UserAgentParser.getDeviceType(userAgent);
            String browser = UserAgentParser.getBrowser(userAgent);
            String os = UserAgentParser.getOperatingSystem(userAgent);

            // Publish access event to Kafka for analytics processing
            publishUrlAccessedEvent(shortCode, ipAddress, userAgent, referer,
                                   deviceType, browser, os);

            // Increment DB click counter
            incrementClickCount(shortCode);

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
                    .deviceType(deviceType)
                    .browser(browser)
                    .operatingSystem(os)
                    .build();

            kafkaTemplate.send(KafkaTopics.URL_ACCESS_EVENTS, shortCode, event);
            log.debug("Published URL accessed event for: {}", shortCode);

        } catch (Exception e) {
            log.error("Failed to publish URL accessed event for {}", shortCode, e);
        }
    }

    /**
     * Increment click count in database
     * Note: Called from trackClickAsync (self-invocation), so relies on trackClickAsync's @Transactional
     */
    public void incrementClickCount(String shortCode) {
        try {
            int updated = urlRepository.incrementClickCount(shortCode);
            if (updated > 0) {
                log.info("Successfully incremented click count for {}: {} rows updated", shortCode, updated);
            } else {
                log.warn("Click count not incremented for {}: no rows matched (shortCode might not exist)", shortCode);
            }
        } catch (Exception e) {
            log.error("Failed to increment click count for {}", shortCode, e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

}
