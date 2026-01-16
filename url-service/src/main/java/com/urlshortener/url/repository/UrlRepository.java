package com.urlshortener.url.repository;

import com.urlshortener.url.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    
    Optional<Url> findByShortCode(String shortCode);
    
    Optional<Url> findByShortCodeAndIsActiveTrue(String shortCode);
    
    List<Url> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Url> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);
    
    boolean existsByShortCode(String shortCode);
    
    long countByUserId(Long userId);
    
    @Modifying
    @Query("UPDATE Url u SET u.isActive = false WHERE u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM Url u WHERE u.expiresAt < :now AND u.isActive = true")
    List<Url> findExpiredUrls(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);
}
