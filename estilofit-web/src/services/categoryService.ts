import { api } from "../lib/api";
import type { Category, CategoryRequest } from "../types/category";

export const categoryService = {
  async list(onlyActive: boolean): Promise<Category[]> {
    const { data } = await api.get<Category[]>("/categories", {
      params: { onlyActive },
    });
    return data;
  },

  async create(request: CategoryRequest): Promise<Category> {
    const { data } = await api.post<Category>("/categories", request);
    return data;
  },

  async rename(id: string, request: CategoryRequest): Promise<Category> {
    const { data } = await api.put<Category>(`/categories/${id}`, request);
    return data;
  },

  async updateStatus(id: string, active: boolean): Promise<Category> {
    const { data } = await api.patch<Category>(`/categories/${id}/status`, { active });
    return data;
  },
};
