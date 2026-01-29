import { create } from 'zustand';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  initAuth: () => void;
}

// Initialize auth state from localStorage before creating the store
const initializeAuth = () => {
  const token = localStorage.getItem('token');
  const userStr = localStorage.getItem('user');

  if (token && userStr) {
    try {
      const user = JSON.parse(userStr);
      return {
        token,
        user,
        isAuthenticated: true,
        isLoading: false,
      };
    } catch (error) {
      console.error('Failed to parse user from localStorage', error);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }

  return {
    token: null,
    user: null,
    isAuthenticated: false,
    isLoading: false,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...initializeAuth(), // Initialize state from localStorage immediately

  login: (token: string, user: User) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    set({ token: null, user: null, isAuthenticated: false });
  },

  initAuth: () => {
    // This method is now deprecated but kept for backwards compatibility
    // Auth is initialized synchronously when the store is created
    const authState = initializeAuth();
    set(authState);
  },
}));
