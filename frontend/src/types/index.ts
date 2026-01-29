// User types
export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// URL types
export interface Url {
  id: number;
  shortCode: string;
  longUrl: string;
  userId: number;
  createdAt: string;
  expiresAt: string | null;
  clickCount: number;
  customAlias: string | null;
}

export interface ShortenUrlRequest {
  longUrl: string;
  customAlias?: string;
  expiresAt?: string;
}

export interface ShortenUrlResponse {
  shortCode: string;
  longUrl: string;
  shortUrl: string;
  createdAt: string;
  expiresAt: string | null;
}

// Analytics types
export interface UrlAnalytics {
  shortCode: string;
  totalClicks: number;
  uniqueIps: number;
  firstClickedAt: string | null;
  lastClickedAt: string | null;
}

export interface DailyAnalytics {
  date: string;
  clickCount: number;
  uniqueIps: number;
}

export interface GeoAnalytics {
  country: string;
  city: string;
  clickCount: number;
}

export interface UrlClick {
  id: number;
  shortCode: string;
  clickedAt: string;
  ipAddress: string;
  userAgent: string;
  referer: string | null;
  country: string | null;
  city: string | null;
  deviceType: string | null;
  browser: string | null;
  os: string | null;
}

export interface TopUrl {
  shortCode: string;
  totalClicks: number;
  longUrl: string;
}
