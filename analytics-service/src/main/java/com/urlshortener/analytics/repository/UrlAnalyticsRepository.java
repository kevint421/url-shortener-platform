package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.UrlAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {
    
    Optional<UrlAnalytics> findByShortCode(String shortCode);
    
    List<UrlAnalytics> findTop10ByOrderByTotalClicksDesc();
    
    List<UrlAnalytics> findByTotalClicksGreaterThan(Long minClicks);
}
