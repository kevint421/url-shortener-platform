package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {
    
    List<UrlClick> findByShortCodeOrderByClickedAtDesc(String shortCode);
    
    List<UrlClick> findByShortCodeAndClickedAtBetween(String shortCode, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(DISTINCT u.ipAddress) FROM UrlClick u WHERE u.shortCode = :shortCode")
    Long countUniqueIpsByShortCode(@Param("shortCode") String shortCode);
    
    long countByShortCode(String shortCode);
}
