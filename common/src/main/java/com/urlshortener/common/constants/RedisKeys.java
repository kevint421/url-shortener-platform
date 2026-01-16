package com.urlshortener.common.constants;

public final class RedisKeys {
    private RedisKeys() {}
    
    public static final String URL_CACHE_PREFIX = "url:";
    public static final String CLICK_COUNTER_PREFIX = "clicks:";
    public static final String RATE_LIMIT_PREFIX = "ratelimit:";
    public static final String USER_RATE_LIMIT_PREFIX = "ratelimit:user:";
    public static final String IP_RATE_LIMIT_PREFIX = "ratelimit:ip:";
    
    public static String urlCacheKey(String shortCode) {
        return URL_CACHE_PREFIX + shortCode;
    }
    
    public static String clickCounterKey(String shortCode) {
        return CLICK_COUNTER_PREFIX + shortCode;
    }
    
    public static String userRateLimitKey(Long userId) {
        return USER_RATE_LIMIT_PREFIX + userId;
    }
    
    public static String ipRateLimitKey(String ipAddress) {
        return IP_RATE_LIMIT_PREFIX + ipAddress;
    }
}