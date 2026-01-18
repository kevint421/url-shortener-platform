package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.UrlDailyAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlDailyAnalyticsRepository extends JpaRepository<UrlDailyAnalytics, Long> {
    
    List<UrlDailyAnalytics> findByShortCodeOrderByDateDesc(String shortCode);
    
    List<UrlDailyAnalytics> findByShortCodeAndDateBetween(String shortCode, LocalDate start, LocalDate end);
    
    Optional<UrlDailyAnalytics> findByShortCodeAndDate(String shortCode, LocalDate date);
}
