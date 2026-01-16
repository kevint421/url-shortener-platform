package com.urlshortener.url.controller;

import com.urlshortener.common.dto.ShortenUrlRequest;
import com.urlshortener.common.dto.ShortenUrlResponse;
import com.urlshortener.common.dto.UrlDetailsResponse;
import com.urlshortener.url.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Slf4j
public class UrlController {
    
    private final UrlService urlService;
    
    /**
     * Shorten a URL
     * POST /api/urls/shorten
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Shortening URL for user {}: {}", userId, request.getLongUrl());
        ShortenUrlResponse response = urlService.shortenUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get URL details
     * GET /api/urls/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlDetailsResponse> getUrlDetails(
            @PathVariable String shortCode,
            @RequestHeader("X-User-Id") Long userId) {
        
        UrlDetailsResponse response = urlService.getUrlDetails(shortCode, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all URLs for a user
     * GET /api/urls/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UrlDetailsResponse>> getUserUrls(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {
        
        // Verify the requesting user matches the path userId
        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<UrlDetailsResponse> urls = urlService.getUserUrls(userId);
        return ResponseEntity.ok(urls);
    }
    
    /**
     * Delete a URL
     * DELETE /api/urls/{shortCode}
     */
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Deleting URL {} for user {}", shortCode, userId);
        urlService.deleteUrl(shortCode, userId);
        return ResponseEntity.noContent().build();
    }
}

