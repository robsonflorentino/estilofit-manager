import { api } from "../lib/api";
import type { PageResponse } from "../types/api";
import type {
  CorrectCostRequest,
  CreateSupplyLotRequest,
  StockAdjustmentRequest,
  StockMovement,
  StockSummaryItem,
  SupplyLotDetail,
  SupplyLotSummary,
} from "../types/inventory";

export const inventoryService = {
  // ── Lotes ────────────────────────────────────────────────────────────
  async listLots(page: number, size: number, supplierId?: string): Promise<PageResponse<SupplyLotSummary>> {
    const { data } = await api.get<PageResponse<SupplyLotSummary>>("/supply-lots", {
      params: { page, size, ...(supplierId ? { supplierId } : {}) },
    });
    return data;
  },

  async getLot(id: string): Promise<SupplyLotDetail> {
    const { data } = await api.get<SupplyLotDetail>(`/supply-lots/${id}`);
    return data;
  },

  async createLot(request: CreateSupplyLotRequest): Promise<SupplyLotDetail> {
    const { data } = await api.post<SupplyLotDetail>("/supply-lots", request);
    return data;
  },

  // ── Estoque ──────────────────────────────────────────────────────────
  async stockSummary(
    page: number,
    size: number,
    filters: { categoryId?: string; lowStock?: boolean },
  ): Promise<PageResponse<StockSummaryItem>> {
    const { data } = await api.get<PageResponse<StockSummaryItem>>("/stock/summary", {
      params: {
        page,
        size,
        ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
        ...(filters.lowStock ? { lowStock: true } : {}),
      },
    });
    return data;
  },

  async movements(page: number, size: number, variantId?: string): Promise<PageResponse<StockMovement>> {
    const { data } = await api.get<PageResponse<StockMovement>>("/stock/movements", {
      params: { page, size, ...(variantId ? { variantId } : {}) },
    });
    return data;
  },

  async adjust(request: StockAdjustmentRequest): Promise<StockMovement> {
    const { data } = await api.post<StockMovement>("/stock/adjustments", request);
    return data;
  },

  async correctCost(request: CorrectCostRequest): Promise<StockMovement> {
    const { data } = await api.post<StockMovement>("/stock/cost-corrections", request);
    return data;
  },
};
