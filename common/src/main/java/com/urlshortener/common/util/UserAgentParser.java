package com.urlshortener.common.util;

public class UserAgentParser {
    
    /**
     * Extract device type from user agent string
     */
    public static String getDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
    
    /**
     * Extract browser from user agent string
     */
    public static String getBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("edg")) {
            return "Edge";
        } else if (ua.contains("chrome")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari")) {
            return "Safari";
        } else if (ua.contains("opera") || ua.contains("opr")) {
            return "Opera";
        } else {
            return "Other";
        }
    }
    
    /**
     * Extract operating system from user agent string
     */
    public static String getOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("ios") || ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        } else {
            return "Other";
        }
    }
}
