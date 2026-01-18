package com.urlshortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_analytics", indexes = {
    @Index(name = "idx_url_analytics_short_code", columnList = "short_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UrlAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10, name = "short_code")
    private String shortCode;
    
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false, name = "total_clicks")
    private Long totalClicks = 0L;
    
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false, name = "unique_ips")
    private Long uniqueIps = 0L;
    
    @Column(name = "last_clicked_at")
    private LocalDateTime lastClickedAt;
    
    @Column(name = "first_clicked_at")
    private LocalDateTime firstClickedAt;
    
    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
