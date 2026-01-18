package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.UrlGeoAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlGeoAnalyticsRepository extends JpaRepository<UrlGeoAnalytics, Long> {
    
    List<UrlGeoAnalytics> findByShortCodeOrderByClickCountDesc(String shortCode);
    
    Optional<UrlGeoAnalytics> findByShortCodeAndCountryAndCity(String shortCode, String country, String city);
    
    List<UrlGeoAnalytics> findByShortCode(String shortCode);
}
