import apiClient from './client';
import type { UrlAnalytics, DailyAnalytics, GeoAnalytics, UrlClick, TopUrl } from '../types';

export const analyticsApi = {
  getUrlAnalytics: async (shortCode: string): Promise<UrlAnalytics> => {
    const response = await apiClient.get<UrlAnalytics>(`/api/analytics/url/${shortCode}`);
    return response.data;
  },

  getDailyAnalytics: async (shortCode: string, days: number = 30): Promise<DailyAnalytics[]> => {
    const response = await apiClient.get<DailyAnalytics[]>(
      `/api/analytics/url/${shortCode}/daily?days=${days}`
    );
    return response.data;
  },

  getGeoAnalytics: async (shortCode: string): Promise<GeoAnalytics[]> => {
    const response = await apiClient.get<GeoAnalytics[]>(`/api/analytics/url/${shortCode}/geo`);
    return response.data;
  },

  getTopUrls: async (limit: number = 10): Promise<TopUrl[]> => {
    const response = await apiClient.get<TopUrl[]>(`/api/analytics/top?limit=${limit}`);
    return response.data;
  },

  getRecentClicks: async (shortCode: string, limit: number = 50): Promise<UrlClick[]> => {
    const response = await apiClient.get<UrlClick[]>(
      `/api/analytics/url/${shortCode}/clicks?limit=${limit}`
    );
    return response.data;
  },
};
