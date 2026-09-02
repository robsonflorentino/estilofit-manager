export interface DashboardKpis {
  monthRevenue: number;
  saleCount: number;
  stockItems: number | null;          // null para vendedor
  estimatedProLabore: number | null;  // null para vendedor
}
