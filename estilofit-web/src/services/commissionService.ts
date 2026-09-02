import { api } from "../lib/api";
import type { CommissionReport } from "../types/commission";

export const commissionService = {
  async report(startDate: string, endDate: string): Promise<CommissionReport> {
    const { data } = await api.get<CommissionReport>("/commissions", {
      params: { startDate, endDate },
    });
    return data;
  },
};
