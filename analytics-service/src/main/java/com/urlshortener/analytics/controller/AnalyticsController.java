package com.urlshortener.analytics.controller;

import com.urlshortener.analytics.entity.UrlClick;
import com.urlshortener.analytics.service.AnalyticsQueryService;
import com.urlshortener.common.dto.DailyAnalyticsResponse;
import com.urlshortener.common.dto.GeoAnalyticsResponse;
import com.urlshortener.common.dto.UrlAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    
    private final AnalyticsQueryService analyticsQueryService;
    
    /**
     * Get analytics summary for a URL
     * GET /api/analytics/url/{shortCode}
     */
    @GetMapping("/url/{shortCode}")
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(@PathVariable String shortCode) {
        log.info("Getting analytics for: {}", shortCode);
        UrlAnalyticsResponse analytics = analyticsQueryService.getUrlAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get daily click statistics
     * GET /api/analytics/url/{shortCode}/daily?days=30
     */
    @GetMapping("/url/{shortCode}/daily")
    public ResponseEntity<List<DailyAnalyticsResponse>> getDailyAnalytics(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("Getting daily analytics for: {} (last {} days)", shortCode, days);
        List<DailyAnalyticsResponse> analytics = analyticsQueryService.getDailyAnalytics(shortCode, days);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get geographic distribution of clicks
     * GET /api/analytics/url/{shortCode}/geo
     */
    @GetMapping("/url/{shortCode}/geo")
    public ResponseEntity<List<GeoAnalyticsResponse>> getGeoAnalytics(@PathVariable String shortCode) {
        log.info("Getting geographic analytics for: {}", shortCode);
        List<GeoAnalyticsResponse> analytics = analyticsQueryService.getGeoAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get top URLs by click count
     * GET /api/analytics/top?limit=10
     */
    @GetMapping("/top")
    public ResponseEntity<List<UrlAnalyticsResponse>> getTopUrls(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting top {} URLs", limit);
        List<UrlAnalyticsResponse> topUrls = analyticsQueryService.getTopUrls(limit);
        return ResponseEntity.ok(topUrls);
    }
    
    /**
     * Get recent clicks for a URL
     * GET /api/analytics/url/{shortCode}/clicks?limit=50
     */
    @GetMapping("/url/{shortCode}/clicks")
    public ResponseEntity<List<UrlClick>> getRecentClicks(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "50") int limit) {
        
        log.info("Getting recent {} clicks for: {}", limit, shortCode);
        List<UrlClick> clicks = analyticsQueryService.getRecentClicks(shortCode, limit);
        return ResponseEntity.ok(clicks);
    }
}