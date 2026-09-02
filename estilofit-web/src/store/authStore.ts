import { create } from "zustand";
import type { Role, UserResponse } from "../types/api";
import { authService } from "../services/authService";
import { setAccessToken } from "../lib/api";

interface AuthState {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitializing: boolean;

  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  restoreSession: () => Promise<void>;
  clear: () => void;
  hasRole: (roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  isInitializing: true,

  login: async (email, password) => {
    set({ isLoading: true });
    try {
      const response = await authService.login(email, password);
      setAccessToken(response.accessToken);
      set({ user: response.user, isAuthenticated: true, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  // Tenta restaurar a sessão no boot usando o refresh token (cookie httpOnly).
  // Se não houver sessão válida, apenas segue como não autenticado.
  restoreSession: async () => {
    try {
      const response = await authService.refresh();
      setAccessToken(response.accessToken);
      set({ user: response.user, isAuthenticated: true, isInitializing: false });
    } catch {
      setAccessToken(null);
      set({ user: null, isAuthenticated: false, isInitializing: false });
    }
  },

  logout: async () => {
    try {
      await authService.logout();
    } finally {
      get().clear();
    }
  },

  clear: () => {
    setAccessToken(null);
    set({ user: null, isAuthenticated: false });
  },

  hasRole: (roles) => {
    const user = get().user;
    return user !== null && roles.includes(user.role);
  },
}));
