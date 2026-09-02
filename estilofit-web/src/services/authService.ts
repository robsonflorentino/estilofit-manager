import { api } from "../lib/api";
import type { LoginResponse, RefreshResponse } from "../types/api";

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>("/auth/login", { email, password });
    return data;
  },

  /** Renova o access token a partir do refresh token (cookie httpOnly). Usado no boot. */
  async refresh(): Promise<RefreshResponse> {
    const { data } = await api.post<RefreshResponse>("/auth/refresh", {});
    return data;
  },

  async logout(): Promise<void> {
    await api.post("/auth/logout");
  },
};
