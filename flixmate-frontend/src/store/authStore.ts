import { create } from 'zustand';

interface UserProfile {
  token: string;
  email: string;
  role: string;
  firstName: string;
  lastName: string;
  userId: string;
}

interface AuthState {
  user: UserProfile | null;
  login: (userData: UserProfile) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: typeof window !== 'undefined' && localStorage.getItem('flixmate_user')
    ? JSON.parse(localStorage.getItem('flixmate_user')!)
    : null,
  
  login: (userData) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('flixmate_user', JSON.stringify(userData));
    }
    set({ user: userData });
  },

  logout: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('flixmate_user');
    }
    set({ user: null });
  },

  isAuthenticated: () => {
    return get().user !== null;
  }
}));
