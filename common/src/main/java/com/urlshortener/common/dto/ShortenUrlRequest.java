package com.urlshortener.common.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlRequest {
    @NotBlank(message = "Long URL is required")
    @Size(max = 2048, message = "URL too long")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String longUrl;
    
    @Size(max = 10, message = "Custom alias too long")
    @Pattern(regexp = "^[a-zA-Z0-9-_]*$", message = "Custom alias can only contain alphanumeric characters, hyphens, and underscores")
    private String customAlias;
    
    private Integer expirationDays;
}