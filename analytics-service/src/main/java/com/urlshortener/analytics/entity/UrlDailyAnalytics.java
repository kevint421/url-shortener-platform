package com.urlshortener.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_daily_analytics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"short_code", "date"}),
       indexes = {
           @Index(name = "idx_url_daily_analytics_short_code_date", columnList = "short_code, date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UrlDailyAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10, name = "short_code")
    private String shortCode;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false, name = "click_count")
    private Long clickCount = 0L;
    
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false, name = "unique_ips")
    private Long uniqueIps = 0L;
    
    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}
