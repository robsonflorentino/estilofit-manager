import { api } from "../lib/api";
import type { StalePromotion } from "../types/promotion";

export const promotionService = {
  async stale(days?: number): Promise<StalePromotion> {
    const { data } = await api.get<StalePromotion>("/promotions/stale", {
      params: days != null ? { days } : {},
    });
    return data;
  },
};
