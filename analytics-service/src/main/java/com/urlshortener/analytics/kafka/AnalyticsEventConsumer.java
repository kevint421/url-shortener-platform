package com.urlshortener.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.analytics.service.AnalyticsProcessingService;
import com.urlshortener.common.constants.KafkaTopics;
import com.urlshortener.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventConsumer {
    
    private final AnalyticsProcessingService analyticsProcessingService;
    private final ObjectMapper objectMapper;
    
    /**
     * Consume URL access events (clicks)
     */
    @KafkaListener(
        topics = KafkaTopics.URL_ACCESS_EVENTS,
        groupId = "analytics-service"
    )
    public void consumeUrlAccessEvent(@Payload Map<String, Object> eventMap) {
        log.debug("Received URL access event: {}", eventMap);
        
        try {
            // Convert Map to UrlAccessedEvent
            UrlAccessedEvent event = objectMapper.convertValue(eventMap, UrlAccessedEvent.class);
            analyticsProcessingService.processUrlAccessEvent(event);
            log.info("Processed URL access event for: {}", event.getShortCode());
        } catch (Exception e) {
            log.error("Error processing URL access event: {}", eventMap, e);
        }
    }
    
    /**
     * Consume URL lifecycle events
     */
    @KafkaListener(
        topics = KafkaTopics.URL_LIFECYCLE_EVENTS,
        groupId = "analytics-service"
    )
    public void consumeUrlLifecycleEvent(@Payload Map<String, Object> eventMap) {
        log.debug("Received URL lifecycle event: {}", eventMap);
        
        try {
            String eventType = (String) eventMap.get("eventType");
            
            if (eventType == null) {
                log.warn("Event type is null, skipping event: {}", eventMap);
                return;
            }
            
            switch (eventType) {
                case "url.created":
                    UrlCreatedEvent createdEvent = objectMapper.convertValue(eventMap, UrlCreatedEvent.class);
                    analyticsProcessingService.processUrlCreatedEvent(createdEvent);
                    break;
                    
                case "url.deleted":
                    UrlDeletedEvent deletedEvent = objectMapper.convertValue(eventMap, UrlDeletedEvent.class);
                    analyticsProcessingService.processUrlDeletedEvent(deletedEvent);
                    break;
                    
                case "url.expired":
                    UrlExpiredEvent expiredEvent = objectMapper.convertValue(eventMap, UrlExpiredEvent.class);
                    analyticsProcessingService.processUrlExpiredEvent(expiredEvent);
                    break;
                    
                default:
                    log.warn("Unknown event type: {}", eventType);
            }
            
            log.info("Processed URL lifecycle event: {}", eventType);
        } catch (Exception e) {
            log.error("Error processing URL lifecycle event: {}", eventMap, e);
        }
    }
}