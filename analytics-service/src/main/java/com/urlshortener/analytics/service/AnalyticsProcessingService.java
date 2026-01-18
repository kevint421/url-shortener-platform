package com.urlshortener.analytics.service;

import com.urlshortener.analytics.entity.UrlAnalytics;
import com.urlshortener.analytics.entity.UrlClick;
import com.urlshortener.analytics.entity.UrlDailyAnalytics;
import com.urlshortener.analytics.entity.UrlGeoAnalytics;
import com.urlshortener.analytics.repository.UrlAnalyticsRepository;
import com.urlshortener.analytics.repository.UrlClickRepository;
import com.urlshortener.analytics.repository.UrlDailyAnalyticsRepository;
import com.urlshortener.analytics.repository.UrlGeoAnalyticsRepository;
import com.urlshortener.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProcessingService {
    
    private final UrlClickRepository urlClickRepository;
    private final UrlAnalyticsRepository urlAnalyticsRepository;
    private final UrlDailyAnalyticsRepository urlDailyAnalyticsRepository;
    private final UrlGeoAnalyticsRepository urlGeoAnalyticsRepository;
    
    /**
     * Process URL access event (click)
     */
    @Transactional
    public void processUrlAccessEvent(UrlAccessedEvent event) {
        String shortCode = event.getShortCode();
        
        // 1. Store raw click event
        UrlClick click = UrlClick.builder()
                .shortCode(shortCode)
                .clickedAt(event.getAccessedAt())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .referer(event.getReferer())
                .country(event.getCountry())
                .city(event.getCity())
                .deviceType(event.getDeviceType())
                .browser(event.getBrowser())
                .operatingSystem(event.getOperatingSystem())
                .build();
        
        urlClickRepository.save(click);
        log.debug("Saved click event for: {}", shortCode);
        
        // 2. Update aggregated analytics
        updateUrlAnalytics(shortCode, event);
        
        // 3. Update daily analytics
        updateDailyAnalytics(shortCode, event);
        
        // 4. Update geo analytics
        updateGeoAnalytics(shortCode, event);
    }
    
    /**
     * Process URL created event
     */
    @Transactional
    public void processUrlCreatedEvent(UrlCreatedEvent event) {
        String shortCode = event.getShortCode();
        
        // Initialize analytics record for new URL
        UrlAnalytics analytics = UrlAnalytics.builder()
                .shortCode(shortCode)
                .totalClicks(0L)
                .uniqueIps(0L)
                .build();
        
        urlAnalyticsRepository.save(analytics);
        log.info("Initialized analytics for new URL: {}", shortCode);
    }
    
    /**
     * Process URL deleted event
     */
    @Transactional
    public void processUrlDeletedEvent(UrlDeletedEvent event) {
        // We keep analytics even when URL is deleted (soft delete pattern)
        log.info("URL deleted, analytics preserved: {}", event.getShortCode());
    }
    
    /**
     * Process URL expired event
     */
    @Transactional
    public void processUrlExpiredEvent(UrlExpiredEvent event) {
        // We keep analytics even when URL expires
        log.info("URL expired, analytics preserved: {}", event.getShortCode());
    }
    
    /**
     * Update aggregated URL analytics
     */
    private void updateUrlAnalytics(String shortCode, UrlAccessedEvent event) {
        UrlAnalytics analytics = urlAnalyticsRepository.findByShortCode(shortCode)
                .orElseGet(() -> UrlAnalytics.builder()
                        .shortCode(shortCode)
                        .totalClicks(0L)
                        .uniqueIps(0L)
                        .build());
        
        // Increment total clicks
        analytics.setTotalClicks(analytics.getTotalClicks() + 1);
        
        // Update timestamps
        if (analytics.getFirstClickedAt() == null) {
            analytics.setFirstClickedAt(event.getAccessedAt());
        }
        analytics.setLastClickedAt(event.getAccessedAt());
        
        // Recalculate unique IPs (could be optimized with a Set in Redis)
        Long uniqueIps = urlClickRepository.countUniqueIpsByShortCode(shortCode);
        analytics.setUniqueIps(uniqueIps);
        
        urlAnalyticsRepository.save(analytics);
        log.debug("Updated analytics for: {} (clicks={}, uniqueIps={})", 
                 shortCode, analytics.getTotalClicks(), uniqueIps);
    }
    
    /**
     * Update daily analytics
     */
    private void updateDailyAnalytics(String shortCode, UrlAccessedEvent event) {
        LocalDate date = event.getAccessedAt().toLocalDate();
        
        UrlDailyAnalytics dailyAnalytics = urlDailyAnalyticsRepository
                .findByShortCodeAndDate(shortCode, date)
                .orElseGet(() -> UrlDailyAnalytics.builder()
                        .shortCode(shortCode)
                        .date(date)
                        .clickCount(0L)
                        .uniqueIps(0L)
                        .build());
        
        // Increment daily clicks
        dailyAnalytics.setClickCount(dailyAnalytics.getClickCount() + 1);
        
        // TODO: simplified approach; more efficient way to track unique IPs per day would be using a bloom filter
        
        urlDailyAnalyticsRepository.save(dailyAnalytics);
        log.debug("Updated daily analytics for: {} on {}", shortCode, date);
    }
    
    /**
     * Update geographic analytics
     */
    private void updateGeoAnalytics(String shortCode, UrlAccessedEvent event) {
        String country = event.getCountry() != null ? event.getCountry() : "Unknown";
        String city = event.getCity() != null ? event.getCity() : "Unknown";
        
        UrlGeoAnalytics geoAnalytics = urlGeoAnalyticsRepository
                .findByShortCodeAndCountryAndCity(shortCode, country, city)
                .orElseGet(() -> UrlGeoAnalytics.builder()
                        .shortCode(shortCode)
                        .country(country)
                        .city(city)
                        .clickCount(0L)
                        .build());
        
        // Increment geographic clicks
        geoAnalytics.setClickCount(geoAnalytics.getClickCount() + 1);
        
        urlGeoAnalyticsRepository.save(geoAnalytics);
        log.debug("Updated geo analytics for: {} in {}, {}", shortCode, city, country);
    }
}
