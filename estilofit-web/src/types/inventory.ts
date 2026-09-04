// ── Lotes de entrada ─────────────────────────────────────────────────────

export interface SupplyLotItemInput {
  variantId: string;
  quantity: number;
  unitCost: number;
}

export interface CreateSupplyLotRequest {
  supplierId: string;
  receivedAt: string; // YYYY-MM-DD
  freightCost: number;
  notes?: string;
  items: SupplyLotItemInput[];
}

export interface SupplyLotItemResponse {
  id: string;
  variant: { id: string; sku: string; size: string; color: string };
  quantity: number;
  unitCost: number;
  freightShare: number;
  realUnitCost: number;
}

export interface SupplyLotSummary {
  id: string;
  supplier: { id: string; name: string };
  receivedAt: string;
  freightCost: number;
  totalCost: number;
  itemCount: number;
  createdBy: { id: string; name: string };
  createdAt: string;
}

export interface SupplyLotDetail {
  id: string;
  supplier: { id: string; name: string };
  receivedAt: string;
  freightCost: number;
  totalCost: number;
  notes: string | null;
  items: SupplyLotItemResponse[];
  createdBy: { id: string; name: string };
  createdAt: string;
}

// ── Estoque ──────────────────────────────────────────────────────────────

export interface StockSummaryItem {
  variantId: string;
  sku: string;
  productName: string;
  category: string;
  size: string;
  color: string;
  stockQuantity: number;
  salePrice: number | null;
  averageCost: number | null;
  isLowStock: boolean;
  isZeroStock: boolean;
}

export type StockMovementType = "ENTRY" | "SALE" | "ADJUSTMENT";

export interface StockMovement {
  id: string;
  variant: { id: string; sku: string; productName: string };
  type: StockMovementType;
  quantity: number;
  referenceType: string | null;
  referenceId: string | null;
  notes: string | null;
  user: { id: string; name: string };
  createdAt: string;
}

export interface StockAdjustmentRequest {
  variantId: string;
  quantity: number;
  notes: string;
}

export interface CorrectCostRequest {
  variantId: string;
  averageCost: number;
  notes: string;
}
