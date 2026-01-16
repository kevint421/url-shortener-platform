package com.urlshortener.common.constants;

public final class AppConstants {
    private AppConstants() {}
    
    public static final String SHORT_CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int SHORT_CODE_LENGTH = 7;
    public static final int BASE62_BASE = 62;
    public static final int MAX_SHORT_CODE_GENERATION_ATTEMPTS = 5;
    public static final String BASE_URL = "http://localhost:8080"; // configurable
}