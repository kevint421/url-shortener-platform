package com.urlshortener.analytics.service;

import com.urlshortener.analytics.entity.UrlAnalytics;
import com.urlshortener.analytics.entity.UrlClick;
import com.urlshortener.analytics.entity.UrlDailyAnalytics;
import com.urlshortener.analytics.entity.UrlGeoAnalytics;
import com.urlshortener.analytics.repository.UrlAnalyticsRepository;
import com.urlshortener.analytics.repository.UrlClickRepository;
import com.urlshortener.analytics.repository.UrlDailyAnalyticsRepository;
import com.urlshortener.analytics.repository.UrlGeoAnalyticsRepository;
import com.urlshortener.common.dto.DailyAnalyticsResponse;
import com.urlshortener.common.dto.GeoAnalyticsResponse;
import com.urlshortener.common.dto.UrlAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {
    
    private final UrlAnalyticsRepository urlAnalyticsRepository;
    private final UrlClickRepository urlClickRepository;
    private final UrlDailyAnalyticsRepository urlDailyAnalyticsRepository;
    private final UrlGeoAnalyticsRepository urlGeoAnalyticsRepository;
    
    /**
     * Get analytics summary for a URL
     */
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getUrlAnalytics(String shortCode) {
        UrlAnalytics analytics = urlAnalyticsRepository.findByShortCode(shortCode)
                .orElse(null);
        
        if (analytics == null) {
            // Return empty analytics if not found
            return UrlAnalyticsResponse.builder()
                    .shortCode(shortCode)
                    .totalClicks(0L)
                    .uniqueIps(0L)
                    .build();
        }
        
        return UrlAnalyticsResponse.builder()
                .shortCode(analytics.getShortCode())
                .totalClicks(analytics.getTotalClicks())
                .uniqueIps(analytics.getUniqueIps())
                .lastClickedAt(analytics.getLastClickedAt())
                .firstClickedAt(analytics.getFirstClickedAt())
                .build();
    }
    
    /**
     * Get daily click statistics for a URL
     */
    @Transactional(readOnly = true)
    public List<DailyAnalyticsResponse> getDailyAnalytics(String shortCode, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<UrlDailyAnalytics> dailyAnalytics = urlDailyAnalyticsRepository
                .findByShortCodeAndDateBetween(shortCode, startDate, endDate);
        
        return dailyAnalytics.stream()
                .map(da -> DailyAnalyticsResponse.builder()
                        .date(da.getDate().toString())
                        .clickCount(da.getClickCount())
                        .uniqueIps(da.getUniqueIps())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get geographic distribution of clicks
     */
    @Transactional(readOnly = true)
    public List<GeoAnalyticsResponse> getGeoAnalytics(String shortCode) {
        List<UrlGeoAnalytics> geoAnalytics = urlGeoAnalyticsRepository
                .findByShortCodeOrderByClickCountDesc(shortCode);
        
        return geoAnalytics.stream()
                .map(ga -> GeoAnalyticsResponse.builder()
                        .country(ga.getCountry())
                        .city(ga.getCity())
                        .clickCount(ga.getClickCount())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get top URLs by click count
     */
    @Transactional(readOnly = true)
    public List<UrlAnalyticsResponse> getTopUrls(int limit) {
        List<UrlAnalytics> topUrls = urlAnalyticsRepository
                .findTop10ByOrderByTotalClicksDesc();
        
        return topUrls.stream()
                .limit(limit)
                .map(analytics -> UrlAnalyticsResponse.builder()
                        .shortCode(analytics.getShortCode())
                        .totalClicks(analytics.getTotalClicks())
                        .uniqueIps(analytics.getUniqueIps())
                        .lastClickedAt(analytics.getLastClickedAt())
                        .firstClickedAt(analytics.getFirstClickedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get recent clicks for a URL
     */
    @Transactional(readOnly = true)
    public List<UrlClick> getRecentClicks(String shortCode, int limit) {
        List<UrlClick> allClicks = urlClickRepository
                .findByShortCodeOrderByClickedAtDesc(shortCode);
        
        return allClicks.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
