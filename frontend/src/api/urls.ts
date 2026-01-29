import apiClient from './client';
import type { Url, ShortenUrlRequest, ShortenUrlResponse } from '../types';

export const urlsApi = {
  shorten: async (data: ShortenUrlRequest): Promise<ShortenUrlResponse> => {
    const response = await apiClient.post<ShortenUrlResponse>('/api/urls/shorten', data);
    return response.data;
  },

  getUrl: async (shortCode: string): Promise<Url> => {
    const response = await apiClient.get<Url>(`/api/urls/${shortCode}`);
    return response.data;
  },

  getUserUrls: async (userId: number): Promise<Url[]> => {
    const response = await apiClient.get<Url[]>(`/api/urls/user/${userId}`);
    return response.data;
  },

  deleteUrl: async (shortCode: string): Promise<void> => {
    await apiClient.delete(`/api/urls/${shortCode}`);
  },
};
