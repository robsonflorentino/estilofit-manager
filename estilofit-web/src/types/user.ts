import type { Role } from "./api";

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  role: Role;
}

export interface UserFilters {
  name?: string;
  role?: Role;
  active?: boolean;
}
