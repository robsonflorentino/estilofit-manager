export interface SellerCommission {
  sellerId: string;
  sellerName: string;
  revenue: number;
  commissionAmount: number;
  saleCount: number;
}

export interface CommissionReport {
  totalCommission: number;
  sellers: SellerCommission[];
}
