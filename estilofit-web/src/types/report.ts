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
