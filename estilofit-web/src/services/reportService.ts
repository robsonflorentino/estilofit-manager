import { api } from "../lib/api";
import type {
  ChannelProfit,
  DailyRevenue,
  ReportPeriod,
  ReportSummary,
  RevenueSlice,
  SalesTarget,
  SellerRanking,
  TopProduct,
} from "../types/report";

export const reportService = {
  async summary(period: ReportPeriod): Promise<ReportSummary> {
    const { data } = await api.get<ReportSummary>("/reports/summary", { params: period });
    return data;
  },

  async revenueByDay(period: ReportPeriod): Promise<DailyRevenue[]> {
    const { data } = await api.get<DailyRevenue[]>("/reports/revenue-by-day", { params: period });
    return data;
  },

  async topProducts(period: ReportPeriod, limit = 10): Promise<TopProduct[]> {
    const { data } = await api.get<TopProduct[]>("/reports/top-products", {
      params: { ...period, limit },
    });
    return data;
  },

  async byChannel(period: ReportPeriod): Promise<RevenueSlice[]> {
    const { data } = await api.get<RevenueSlice[]>("/reports/by-channel", { params: period });
    return data;
  },

  async byPayment(period: ReportPeriod): Promise<RevenueSlice[]> {
    const { data } = await api.get<RevenueSlice[]>("/reports/by-payment", { params: period });
    return data;
  },

  async salesTarget(months: number): Promise<SalesTarget> {
    const { data } = await api.get<SalesTarget>("/reports/sales-target", { params: { months } });
    return data;
  },

  async profitByChannel(period: ReportPeriod): Promise<ChannelProfit[]> {
    const { data } = await api.get<ChannelProfit[]>("/reports/profit-by-channel", { params: period });
    return data;
  },

  async sellerRanking(period: ReportPeriod): Promise<SellerRanking[]> {
    const { data } = await api.get<SellerRanking[]>("/reports/seller-ranking", { params: period });
    return data;
  },
};
