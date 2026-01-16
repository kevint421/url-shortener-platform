package com.urlshortener.common.event;

public enum EventType {
    URL_CREATED("url.created"),
    URL_DELETED("url.deleted"),
    URL_EXPIRED("url.expired"),
    URL_ACCESSED("url.accessed");
    
    private final String value;
    
    EventType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}