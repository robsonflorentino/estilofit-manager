import { api } from "../lib/api";
import type { LoginResponse } from "../types/api";

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>("/auth/login", { email, password });
    return data;
  },

  async logout(): Promise<void> {
    await api.post("/auth/logout");
  },
};
