package com.urlshortener.url.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final UrlService urlService;
    private final AsyncClickTrackerService asyncClickTrackerService;

    /**
     * Handle redirect - optimized for performance
     * This is the HOT PATH - must be < 50ms
     */
    public String redirect(String shortCode, HttpServletRequest request) {
        // Get long URL (cached or from DB)
        String longUrl = urlService.getLongUrl(shortCode);

        // Extract request data SYNCHRONOUSLY before going async
        // HttpServletRequest gets recycled after response is sent, so we can't pass it to @Async methods
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        // Delegate to separate service for async click tracking
        // Pass extracted data (not the request object) to avoid recycled request issues
        asyncClickTrackerService.trackClickAsync(shortCode, ipAddress, userAgent, referer);

        return longUrl;
    }

    /**
     * Extract client IP address from request
     * Must be called synchronously before request is recycled
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}