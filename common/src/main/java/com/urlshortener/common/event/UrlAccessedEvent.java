package com.urlshortener.common.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAccessedEvent extends BaseEvent {
    private String shortCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime accessedAt;
    
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String city;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    
    public UrlAccessedEvent(String eventId, LocalDateTime timestamp, String eventType,
                           String shortCode, LocalDateTime accessedAt, String ipAddress,
                           String userAgent, String referer, String country, String city,
                           String deviceType, String browser, String operatingSystem) {
        super(eventId, timestamp, eventType);
        this.shortCode = shortCode;
        this.accessedAt = accessedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.country = country;
        this.city = city;
        this.deviceType = deviceType;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
    }
}