import { api } from "../lib/api";
import type { PageResponse, UserResponse } from "../types/api";
import type { CreateUserRequest, UpdateUserRequest, UserFilters } from "../types/user";

export const userService = {
  async list(
    page: number,
    size: number,
    filters: UserFilters,
  ): Promise<PageResponse<UserResponse>> {
    const { data } = await api.get<PageResponse<UserResponse>>("/users", {
      params: {
        page,
        size,
        ...(filters.name ? { name: filters.name } : {}),
        ...(filters.role ? { role: filters.role } : {}),
        ...(filters.active !== undefined ? { active: filters.active } : {}),
      },
    });
    return data;
  },

  async create(request: CreateUserRequest): Promise<UserResponse> {
    const { data } = await api.post<UserResponse>("/users", request);
    return data;
  },

  async update(id: string, request: UpdateUserRequest): Promise<UserResponse> {
    const { data } = await api.put<UserResponse>(`/users/${id}`, request);
    return data;
  },

  async updateStatus(id: string, active: boolean): Promise<UserResponse> {
    const { data } = await api.patch<UserResponse>(`/users/${id}/status`, { active });
    return data;
  },

  async resetPassword(id: string, newPassword: string): Promise<void> {
    await api.patch(`/users/${id}/password`, { newPassword });
  },
};
