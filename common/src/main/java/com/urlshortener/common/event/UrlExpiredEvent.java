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
public class UrlExpiredEvent extends BaseEvent {
    private String shortCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
    
    public UrlExpiredEvent(String eventId, LocalDateTime timestamp, String eventType,
                          String shortCode, LocalDateTime expiredAt) {
        super(eventId, timestamp, eventType);
        this.shortCode = shortCode;
        this.expiredAt = expiredAt;
    }
}