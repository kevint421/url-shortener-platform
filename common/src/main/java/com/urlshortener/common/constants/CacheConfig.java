package com.urlshortener.common.constants;

public final class CacheConfig {
    private CacheConfig() {}
    
    public static final long URL_CACHE_TTL_SECONDS = 3600; // 1 hr
    public static final long RATE_LIMIT_WINDOW_SECONDS = 60; // 1 min
    public static final int MAX_REQUESTS_PER_MINUTE = 100;
    public static final int MAX_URLS_PER_USER = 1000;
}