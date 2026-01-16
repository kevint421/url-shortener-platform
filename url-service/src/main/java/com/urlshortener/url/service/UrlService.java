package com.urlshortener.url.service;

import com.urlshortener.common.constants.AppConstants;
import com.urlshortener.common.constants.CacheConfig;
import com.urlshortener.common.constants.KafkaTopics;
import com.urlshortener.common.constants.RedisKeys;
import com.urlshortener.common.dto.ShortenUrlRequest;
import com.urlshortener.common.dto.ShortenUrlResponse;
import com.urlshortener.common.dto.UrlDetailsResponse;
import com.urlshortener.common.event.EventType;
import com.urlshortener.common.event.UrlCreatedEvent;
import com.urlshortener.common.event.UrlDeletedEvent;
import com.urlshortener.common.util.EventIdGenerator;
import com.urlshortener.common.util.ShortCodeGenerator;
import com.urlshortener.url.entity.Url;
import com.urlshortener.url.exception.UrlNotFoundException;
import com.urlshortener.url.exception.UrlExpiredException;
import com.urlshortener.url.exception.DuplicateShortCodeException;
import com.urlshortener.url.exception.UserLimitExceededException;
import com.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {
    
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Shorten a URL
     */
    @Transactional
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request, Long userId) {
        // Check user URL limit
        long userUrlCount = urlRepository.countByUserId(userId);
        if (userUrlCount >= CacheConfig.MAX_URLS_PER_USER) {
            throw new UserLimitExceededException("User has reached maximum URL limit");
        }
        
        // Generate or use custom short code
        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            shortCode = request.getCustomAlias();
            
            // Validate custom alias
            if (!ShortCodeGenerator.isValidShortCode(shortCode)) {
                throw new IllegalArgumentException("Invalid custom alias format");
            }
            
            // Check if already exists
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new DuplicateShortCodeException("Custom alias already exists");
            }
        } else {
            shortCode = generateUniqueShortCode();
        }
        
        // Calculate expiration
        LocalDateTime expiresAt = null;
        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpirationDays());
        }
        
        // Create URL entity
        Url url = Url.builder()
                .shortCode(shortCode)
                .longUrl(request.getLongUrl())
                .userId(userId)
                .expiresAt(expiresAt)
                .customAlias(request.getCustomAlias() != null && !request.getCustomAlias().isEmpty())
                .build();
        
        url = urlRepository.save(url);
        
        // Cache in Redis
        cacheUrl(shortCode, request.getLongUrl());
        
        // Publish Kafka event
        publishUrlCreatedEvent(url);
        
        log.info("URL shortened: {} -> {}", request.getLongUrl(), shortCode);
        
        return ShortenUrlResponse.builder()
                .shortCode(shortCode)
                .shortUrl(AppConstants.BASE_URL + "/" + shortCode)
                .longUrl(request.getLongUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }
    
    /**
     * Get long URL from short code (for redirect)
     */
    public String getLongUrl(String shortCode) {
        // Try Redis cache first
        String cachedUrl = getCachedUrl(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {}", shortCode);
            return cachedUrl;
        }
        
        // Cache miss - query database
        log.debug("Cache miss for short code: {}", shortCode);
        Url url = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        
        // Check expiration
        if (url.isExpired()) {
            throw new UrlExpiredException("URL has expired: " + shortCode);
        }
        
        // Cache for future requests
        cacheUrl(shortCode, url.getLongUrl());
        
        return url.getLongUrl();
    }
    
    /**
     * Get URL details
     */
    @Transactional(readOnly = true)
    public UrlDetailsResponse getUrlDetails(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        
        // Verify ownership
        if (!url.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to URL");
        }
        
        return mapToDetailsResponse(url);
    }
    
    /**
     * Get all URLs for a user
     */
    @Transactional(readOnly = true)
    public List<UrlDetailsResponse> getUserUrls(Long userId) {
        List<Url> urls = urlRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return urls.stream()
                .map(this::mapToDetailsResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete a URL
     */
    @Transactional
    public void deleteUrl(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
        
        // Verify ownership
        if (!url.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to URL");
        }
        
        // Soft delete
        url.setIsActive(false);
        urlRepository.save(url);
        
        // Remove from cache
        removeCachedUrl(shortCode);
        
        // Publish deletion event
        publishUrlDeletedEvent(url, userId);
        
        log.info("URL deleted: {}", shortCode);
    }
    
    /**
     * Generate unique short code
     */
    private String generateUniqueShortCode() {
        int attempts = 0;
        while (attempts < AppConstants.MAX_SHORT_CODE_GENERATION_ATTEMPTS) {
            String shortCode = ShortCodeGenerator.generateRandom();
            if (!urlRepository.existsByShortCode(shortCode)) {
                return shortCode;
            }
            attempts++;
        }
        throw new IllegalStateException("Failed to generate unique short code");
    }
    
    /**
     * Cache URL in Redis
     */
    private void cacheUrl(String shortCode, String longUrl) {
        try {
            String key = RedisKeys.urlCacheKey(shortCode);
            redisTemplate.opsForValue().set(
                key, 
                longUrl, 
                CacheConfig.URL_CACHE_TTL_SECONDS, 
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to cache URL: {}", shortCode, e);
            // Don't fail the request if caching fails
        }
    }
    
    /**
     * Get cached URL from Redis
     */
    private String getCachedUrl(String shortCode) {
        try {
            String key = RedisKeys.urlCacheKey(shortCode);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cached URL: {}", shortCode, e);
            return null;
        }
    }
    
    /**
     * Remove URL from cache
     */
    private void removeCachedUrl(String shortCode) {
        try {
            String key = RedisKeys.urlCacheKey(shortCode);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to remove cached URL: {}", shortCode, e);
        }
    }
    
    /**
     * Publish URL created event to Kafka
     */
    private void publishUrlCreatedEvent(Url url) {
        try {
            UrlCreatedEvent event = UrlCreatedEvent.builder()
                    .eventId(EventIdGenerator.generate())
                    .timestamp(LocalDateTime.now())
                    .eventType(EventType.URL_CREATED.getValue())
                    .shortCode(url.getShortCode())
                    .longUrl(url.getLongUrl())
                    .userId(url.getUserId())
                    .createdAt(url.getCreatedAt())
                    .expiresAt(url.getExpiresAt())
                    .customAlias(url.getCustomAlias())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.URL_LIFECYCLE_EVENTS, url.getShortCode(), event);
            log.debug("Published URL created event: {}", url.getShortCode());
        } catch (Exception e) {
            log.error("Failed to publish URL created event", e);
            // Don't fail the request if Kafka fails
        }
    }
    
    /**
     * Publish URL deleted event to Kafka
     */
    private void publishUrlDeletedEvent(Url url, Long userId) {
        try {
            UrlDeletedEvent event = UrlDeletedEvent.builder()
                    .eventId(EventIdGenerator.generate())
                    .timestamp(LocalDateTime.now())
                    .eventType(EventType.URL_DELETED.getValue())
                    .shortCode(url.getShortCode())
                    .userId(userId)
                    .deletedAt(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(KafkaTopics.URL_LIFECYCLE_EVENTS, url.getShortCode(), event);
            log.debug("Published URL deleted event: {}", url.getShortCode());
        } catch (Exception e) {
            log.error("Failed to publish URL deleted event", e);
        }
    }
    
    /**
     * Map URL entity to details response
     */
    private UrlDetailsResponse mapToDetailsResponse(Url url) {
        return UrlDetailsResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .longUrl(url.getLongUrl())
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .isActive(url.getIsActive())
                .customAlias(url.getCustomAlias())
                .build();
    }
}

