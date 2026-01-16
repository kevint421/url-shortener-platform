package com.urlshortener.common.util;

import org.apache.commons.lang3.RandomStringUtils;

import com.urlshortener.common.constants.AppConstants;

public class ShortCodeGenerator {
    
    private static final String CHARACTERS = AppConstants.SHORT_CODE_CHARACTERS;
    private static final int BASE = AppConstants.BASE62_BASE;
    
    /**
     * Generate a short code using Base62 encoding of a number
     */
    public static String generateFromId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        
        StringBuilder shortCode = new StringBuilder();
        long num = id;
        
        while (num > 0) {
            int remainder = (int) (num % BASE);
            shortCode.insert(0, CHARACTERS.charAt(remainder));
            num = num / BASE;
        }
        
        // Pad with leading characters if needed to reach minimum length
        while (shortCode.length() < AppConstants.SHORT_CODE_LENGTH) {
            shortCode.insert(0, CHARACTERS.charAt(0));
        }
        
        return shortCode.toString();
    }
    
    /**
     * Generate a random short code
     */
    public static String generateRandom() {
        return RandomStringUtils.random(
            AppConstants.SHORT_CODE_LENGTH, 
            CHARACTERS
        );
    }
    
    /**
     * Validate if a string is a valid short code
     */
    public static boolean isValidShortCode(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            return false;
        }
        
        if (shortCode.length() > 10) {
            return false;
        }
        
        for (char c : shortCode.toCharArray()) {
            if (CHARACTERS.indexOf(c) == -1) {
                return false;
            }
        }
        
        return true;
    }
}