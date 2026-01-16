package com.urlshortener.url.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_urls_short_code", columnList = "short_code"),
    @Index(name = "idx_urls_user_id", columnList = "user_id"),
    @Index(name = "idx_urls_created_at", columnList = "created_at"),
    @Index(name = "idx_urls_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Url {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10, name = "short_code")
    private String shortCode;
    
    @Column(nullable = false, columnDefinition = "TEXT", name = "long_url")
    private String longUrl;
    
    @Column(nullable = false, name = "user_id")
    private Long userId;
    
    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Builder.Default  
    @ColumnDefault("0")
    @Column(nullable = false, name = "click_count")
    private Long clickCount = 0L;
    
    @Builder.Default  
    @ColumnDefault("true")  
    @Column(nullable = false, name = "is_active")
    private Boolean isActive = true;
    
    @Builder.Default 
    @ColumnDefault("false") 
    @Column(nullable = false, name = "custom_alias")
    private Boolean customAlias = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    /**
     * Check if URL has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Increment click count
     */
    public void incrementClickCount() {
        this.clickCount++;
    }
}