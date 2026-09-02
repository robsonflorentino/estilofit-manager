import { api } from "../lib/api";
import type { DashboardKpis } from "../types/dashboard";

export const dashboardService = {
  async getKpis(): Promise<DashboardKpis> {
    const { data } = await api.get<DashboardKpis>("/dashboard/kpis");
    return data;
  },
};
