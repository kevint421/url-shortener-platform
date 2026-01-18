package com.urlshortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_clicks", indexes = {
    @Index(name = "idx_url_clicks_short_code", columnList = "short_code"),
    @Index(name = "idx_url_clicks_clicked_at", columnList = "clicked_at"),
    @Index(name = "idx_url_clicks_ip_address", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UrlClick {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10, name = "short_code")
    private String shortCode;
    
    @CreatedDate
    @Column(nullable = false, name = "clicked_at")
    private LocalDateTime clickedAt;
    
    @Column(length = 45, name = "ip_address")
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT", name = "user_agent")
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String referer;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 50, name = "device_type")
    private String deviceType;
    
    @Column(length = 50)
    private String browser;
    
    @Column(length = 50, name = "operating_system")
    private String operatingSystem;
}