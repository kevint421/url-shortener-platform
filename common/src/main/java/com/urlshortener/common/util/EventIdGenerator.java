package com.urlshortener.common.util;

import java.util.UUID;

public class EventIdGenerator {
    
    /**
     * Generate a unique event ID
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}