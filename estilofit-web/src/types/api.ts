// ── Resposta de erro padrão da API (ADR-009) ─────────────────────────

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}

// ── Auth ──────────────────────────────────────────────────────────────

export type Role = "ADMIN" | "MANAGER" | "SELLER";

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface RefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

// ── Paginação (ADR-010) ──────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
