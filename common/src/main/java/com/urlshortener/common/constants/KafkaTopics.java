package com.urlshortener.common.constants;

public final class KafkaTopics {
    private KafkaTopics() {}
    
    public static final String URL_LIFECYCLE_EVENTS = "url-lifecycle-events";
    public static final String URL_ACCESS_EVENTS = "url-access-events";
}