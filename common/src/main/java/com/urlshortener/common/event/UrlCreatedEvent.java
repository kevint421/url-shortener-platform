package com.urlshortener.common.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlCreatedEvent extends BaseEvent {
    private String shortCode;
    private String longUrl;
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    private Boolean customAlias;
    
    public UrlCreatedEvent(String eventId, LocalDateTime timestamp, String eventType,
                          String shortCode, String longUrl, Long userId,
                          LocalDateTime createdAt, LocalDateTime expiresAt, Boolean customAlias) {
        super(eventId, timestamp, eventType);
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.customAlias = customAlias;
    }
}