export interface StaleProduct {
  variantId: string;
  sku: string;
  productName: string;
  size: string;
  color: string;
  stockQuantity: number;
  salePrice: number | null;
  averageCost: number | null;
  lastSaleAt: string | null; // ISO datetime, null = nunca vendeu
  daysStale: number;
  neverSold: boolean;
  stockValue: number;
}

export interface StalePromotion {
  thresholdDays: number;
  staleCount: number;
  totalStockValue: number;
  items: StaleProduct[];
}
