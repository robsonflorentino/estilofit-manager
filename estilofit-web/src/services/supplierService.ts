import { api } from "../lib/api";
import type { PageResponse } from "../types/api";
import type { Supplier, SupplierFilters, SupplierRequest } from "../types/supplier";

export const supplierService = {
  async list(
    page: number,
    size: number,
    filters: SupplierFilters,
  ): Promise<PageResponse<Supplier>> {
    const { data } = await api.get<PageResponse<Supplier>>("/suppliers", {
      params: {
        page,
        size,
        ...(filters.name ? { name: filters.name } : {}),
        ...(filters.active !== undefined ? { active: filters.active } : {}),
      },
    });
    return data;
  },

  async create(request: SupplierRequest): Promise<Supplier> {
    const { data } = await api.post<Supplier>("/suppliers", request);
    return data;
  },

  async update(id: string, request: SupplierRequest): Promise<Supplier> {
    const { data } = await api.put<Supplier>(`/suppliers/${id}`, request);
    return data;
  },

  async updateStatus(id: string, active: boolean): Promise<Supplier> {
    const { data } = await api.patch<Supplier>(`/suppliers/${id}/status`, { active });
    return data;
  },
};
