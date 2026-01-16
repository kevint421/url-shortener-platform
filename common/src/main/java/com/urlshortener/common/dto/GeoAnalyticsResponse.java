package com.urlshortener.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoAnalyticsResponse {
    private String country;
    private String city;
    private Long clickCount;
}