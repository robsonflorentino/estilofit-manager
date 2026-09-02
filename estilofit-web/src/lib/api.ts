import axios, { AxiosError } from "axios";
import toast from "react-hot-toast";
import type { ApiErrorResponse, RefreshResponse } from "../types/api";

const baseURL = import.meta.env.VITE_API_BASE_URL as string;

export const api = axios.create({
  baseURL,
  withCredentials: true, // envia o httpOnly cookie do refresh token
});

// ── Gestão do access token (em memória — nunca em localStorage) ──────────
let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

// Callbacks registrados pela camada de auth para reagir a logout/sessão expirada
let onSessionExpired: (() => void) | null = null;

export function registerSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
}

// ── Request interceptor: injeta o Bearer token ───────────────────────────
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ── Response interceptor: tratamento global de erros (ADR-009) ───────────
let isRefreshing = false;

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status;
    const originalRequest = error.config;

    // 401 — tenta renovar o token silenciosamente uma vez
    if (status === 401 && originalRequest && !originalRequest.headers["X-Retry"]) {
      // Não tenta refresh no próprio endpoint de login/refresh
      const url = originalRequest.url ?? "";
      if (url.includes("/auth/login") || url.includes("/auth/refresh")) {
        return Promise.reject(error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        try {
          const { data } = await axios.post<RefreshResponse>(
            `${baseURL}/auth/refresh`,
            {},
            { withCredentials: true },
          );
          setAccessToken(data.accessToken);
          isRefreshing = false;

          // Reexecuta a requisição original com o novo token
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          originalRequest.headers["X-Retry"] = "true";
          return api.request(originalRequest);
        } catch {
          isRefreshing = false;
          setAccessToken(null);
          onSessionExpired?.();
          return Promise.reject(error);
        }
      }
    }

    // 403 — sem permissão (toast, sem redirecionar)
    if (status === 403) {
      toast.error("Você não tem permissão para realizar esta ação.");
    }

    // 500 — erro de servidor (toast genérico)
    if (status !== undefined && status >= 500) {
      toast.error("Erro interno do servidor. Tente novamente em instantes.");
    }

    return Promise.reject(error);
  },
);

// ── Helper para extrair mensagem de erro da API ──────────────────────────
export function getApiErrorMessage(error: unknown, fallback = "Ocorreu um erro."): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}
