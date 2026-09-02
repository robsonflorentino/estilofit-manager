import { api } from "../lib/api";
import type { PageResponse } from "../types/api";
import type {
  CreateProductRequest,
  CreateVariantRequest,
  ProductDetail,
  ProductFilters,
  ProductSummary,
  UpdateVariantRequest,
  Variant,
} from "../types/product";

export const productService = {
  async list(
    page: number,
    size: number,
    filters: ProductFilters,
  ): Promise<PageResponse<ProductSummary>> {
    const { data } = await api.get<PageResponse<ProductSummary>>("/products", {
      params: {
        page,
        size,
        ...(filters.name ? { name: filters.name } : {}),
        ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
        ...(filters.active !== undefined ? { active: filters.active } : {}),
      },
    });
    return data;
  },

  async getById(id: string): Promise<ProductDetail> {
    const { data } = await api.get<ProductDetail>(`/products/${id}`);
    return data;
  },

  async create(request: CreateProductRequest): Promise<ProductDetail> {
    const { data } = await api.post<ProductDetail>("/products", request);
    return data;
  },

  async update(id: string, request: CreateProductRequest): Promise<ProductDetail> {
    const { data } = await api.put<ProductDetail>(`/products/${id}`, request);
    return data;
  },

  async updateStatus(id: string, active: boolean): Promise<ProductDetail> {
    const { data } = await api.patch<ProductDetail>(`/products/${id}/status`, { active });
    return data;
  },

  // ── Variações ──────────────────────────────────────────────────────────
  async createVariant(productId: string, request: CreateVariantRequest): Promise<Variant> {
    const { data } = await api.post<Variant>(`/products/${productId}/variants`, request);
    return data;
  },

  async updateVariant(
    productId: string,
    variantId: string,
    request: UpdateVariantRequest,
  ): Promise<Variant> {
    const { data } = await api.put<Variant>(`/products/${productId}/variants/${variantId}`, request);
    return data;
  },

  async updateVariantStatus(
    productId: string,
    variantId: string,
    active: boolean,
  ): Promise<Variant> {
    const { data } = await api.patch<Variant>(
      `/products/${productId}/variants/${variantId}/status`,
      { active },
    );
    return data;
  },
};
