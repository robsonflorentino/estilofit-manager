export interface ReportSummary {
  revenue: number;
  saleCount: number;
  averageTicket: number;
  estimatedProfit: number;
}

export interface DailyRevenue {
  day: string; // YYYY-MM-DD
  revenue: number;
  saleCount: number;
}

export interface TopProduct {
  variantId: string;
  sku: string;
  productName: string;
  size: string;
  color: string;
  quantity: number;
  revenue: number;
}

export interface RevenueSlice {
  label: string;
  revenue: number;
  saleCount: number;
  percentage: number;
}

export interface ReportPeriod {
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
}

export interface SalesTargetMonth {
  month: string; // YYYY-MM
  revenue: number;
  target: number;
  profitMarginPct: number;
  achieved: boolean;
}

export interface SalesTarget {
  targetProLabore: number;
  proLaborePct: number;
  months: SalesTargetMonth[];
}

export interface ChannelProfit {
  channel: string;
  revenue: number;
  cost: number;
  profit: number;
  marginPct: number;
  saleCount: number;
}

export interface SellerRanking {
  position: number;
  sellerId: string;
  sellerName: string;
  revenue: number;
  saleCount: number;
  averageTicket: number;
}

export interface PurchaseSuggestion {
  variantId: string;
  sku: string;
  productName: string;
  size: string;
  color: string;
  stockQuantity: number;
  soldQty: number;
  dailyVelocity: number;
  coverageDays: number | null;
  suggestedQty: number;
  belowMinimum: boolean;
  estimatedCost: number;
}

export interface PurchaseSuggestionReport {
  referenceDays: number;
  coverageTargetDays: number;
  totalItems: number;
  totalEstimatedCost: number;
  items: PurchaseSuggestion[];
}
