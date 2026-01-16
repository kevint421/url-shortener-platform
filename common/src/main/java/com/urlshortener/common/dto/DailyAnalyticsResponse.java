package com.urlshortener.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAnalyticsResponse {
    private String date;
    private Long clickCount;
    private Long uniqueIps;
}