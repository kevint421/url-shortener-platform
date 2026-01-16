package com.urlshortener.url.controller;

import com.urlshortener.url.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RedirectController {
    
    private final RedirectService redirectService;
    
    /**
     * Redirect short URL to long URL
     * GET /{shortCode}
     * 
     * This is the HOT PATH - optimized for < 50ms response time
     */
    @GetMapping("/{shortCode}")
    public void redirect(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Get long URL (from cache or DB)
            String longUrl = redirectService.redirect(shortCode, request);
            
            // Send 301 redirect (permanent redirect for SEO)
            response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
            response.setHeader("Location", longUrl);
            response.setHeader("Cache-Control", "no-cache");
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Redirect {} -> {} took {}ms", shortCode, longUrl, duration);
            
            if (duration > 50) {
                log.warn("Slow redirect detected: {}ms for {}", duration, shortCode);
            }
            
        } catch (Exception e) {
            log.error("Redirect failed for {}", shortCode, e);
            response.sendError(HttpStatus.NOT_FOUND.value(), "URL not found");
        }
    }
}
