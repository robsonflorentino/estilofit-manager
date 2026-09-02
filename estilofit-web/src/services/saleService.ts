import { api } from "../lib/api";
import type { PageResponse } from "../types/api";
import type {
  CancelSaleRequest,
  CreateSaleRequest,
  InstallmentFilters,
  InstallmentResponse,
  InstallmentWithSale,
  ProjectedMonth,
  SaleChannel,
  SaleDetail,
  SaleFilters,
  SaleSummary,
} from "../types/sale";

export const saleService = {
  // ── Vendas ─────────────────────────────────────────────────────────────
  async list(page: number, size: number, filters: SaleFilters): Promise<PageResponse<SaleSummary>> {
    const { data } = await api.get<PageResponse<SaleSummary>>("/sales", {
      params: {
        page,
        size,
        ...(filters.channelId ? { channelId: filters.channelId } : {}),
        ...(filters.paymentMethod ? { paymentMethod: filters.paymentMethod } : {}),
        ...(filters.status ? { status: filters.status } : {}),
      },
    });
    return data;
  },

  async getById(id: string): Promise<SaleDetail> {
    const { data } = await api.get<SaleDetail>(`/sales/${id}`);
    return data;
  },

  async create(request: CreateSaleRequest): Promise<SaleDetail> {
    const { data } = await api.post<SaleDetail>("/sales", request);
    return data;
  },

  async cancel(id: string, request: CancelSaleRequest): Promise<SaleDetail> {
    const { data } = await api.patch<SaleDetail>(`/sales/${id}/cancel`, request);
    return data;
  },

  // ── Contas a receber ─────────────────────────────────────────────────────
  async listInstallments(
    page: number,
    size: number,
    filters: InstallmentFilters,
  ): Promise<PageResponse<InstallmentWithSale>> {
    const { data } = await api.get<PageResponse<InstallmentWithSale>>("/installments", {
      params: {
        page,
        size,
        ...(filters.status ? { status: filters.status } : {}),
        ...(filters.saleId ? { saleId: filters.saleId } : {}),
        ...(filters.startDue ? { startDue: filters.startDue } : {}),
        ...(filters.endDue ? { endDue: filters.endDue } : {}),
      },
    });
    return data;
  },

  async projected(months: number): Promise<ProjectedMonth[]> {
    const { data } = await api.get<ProjectedMonth[]>("/installments/projected", {
      params: { months },
    });
    return data;
  },

  async receiveInstallment(id: string, receivedAt?: string): Promise<InstallmentResponse> {
    const { data } = await api.patch<InstallmentResponse>(
      `/installments/${id}/receive`,
      receivedAt ? { receivedAt } : {},
    );
    return data;
  },

  // ── Canais de venda ─────────────────────────────────────────────────────
  async listChannels(includeInactive = false): Promise<SaleChannel[]> {
    const { data } = await api.get<SaleChannel[]>("/sale-channels", {
      params: { includeInactive },
    });
    return data;
  },
};
