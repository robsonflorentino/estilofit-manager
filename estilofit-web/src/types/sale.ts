// ── Enums (espelham o backend) ─────────────────────────────────────────────

export type PaymentMethod = "CASH" | "PIX" | "DEBIT_CARD" | "CREDIT_CARD" | "TRANSFER";
export type SaleStatus = "CONFIRMED" | "CANCELLED";
export type InstallmentStatus = "PENDING" | "RECEIVED" | "CANCELLED";

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Dinheiro",
  PIX: "Pix",
  DEBIT_CARD: "Cartão de débito",
  CREDIT_CARD: "Cartão de crédito",
  TRANSFER: "Transferência",
};

export const SALE_STATUS_LABELS: Record<SaleStatus, string> = {
  CONFIRMED: "Confirmada",
  CANCELLED: "Cancelada",
};

export const INSTALLMENT_STATUS_LABELS: Record<InstallmentStatus, string> = {
  PENDING: "Pendente",
  RECEIVED: "Recebida",
  CANCELLED: "Cancelada",
};

// ── Referências ─────────────────────────────────────────────────────────────

export interface ChannelRef {
  id: string;
  name: string;
}

export interface SellerRef {
  id: string;
  name: string;
}

export interface SaleVariantRef {
  id: string;
  sku: string;
  productName: string;
  size: string;
  color: string;
}

// ── Requests ──────────────────────────────────────────────────────────────

export interface CreateSaleItemRequest {
  variantId: string;
  quantity: number;
}

export interface CreateSaleRequest {
  channelId: string;
  paymentMethod: PaymentMethod;
  installments: number;
  cardFeePct?: number | null;
  cardFeePassed: boolean;
  discountAmount: number;
  notes?: string;
  items: CreateSaleItemRequest[];
}

export interface CancelSaleRequest {
  reason: string;
}

// ── Responses ───────────────────────────────────────────────────────────────

export interface SaleItemResponse {
  id: string;
  variant: SaleVariantRef;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface InstallmentResponse {
  id: string;
  installmentNum: number;
  dueDate: string; // YYYY-MM-DD
  grossAmount: number;
  netAmount: number;
  status: InstallmentStatus;
  receivedAt: string | null;
}

export interface SaleSummary {
  id: string;
  channel: ChannelRef;
  seller: SellerRef;
  confirmedAt: string;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  paymentMethod: PaymentMethod;
  installments: number;
  status: SaleStatus;
  itemCount: number;
}

export interface SaleDetail {
  id: string;
  channel: ChannelRef;
  seller: SellerRef;
  confirmedAt: string;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  paymentMethod: PaymentMethod;
  installments: number;
  cardFeePct: number | null;
  cardFeePassed: boolean;
  commissionPct: number;
  commissionAmount: number;
  status: SaleStatus;
  notes: string | null;
  items: SaleItemResponse[];
  installmentSchedule: InstallmentResponse[];
  cancelledAt: string | null;
}

export interface SaleFilters {
  channelId?: string;
  paymentMethod?: PaymentMethod;
  status?: SaleStatus;
}

// ── Contas a receber ─────────────────────────────────────────────────────────

export interface InstallmentSaleRef {
  id: string;
  confirmedAt: string;
  finalAmount: number;
}

export interface InstallmentWithSale {
  id: string;
  installmentNum: number;
  dueDate: string;
  grossAmount: number;
  netAmount: number;
  status: InstallmentStatus;
  receivedAt: string | null;
  sale: InstallmentSaleRef;
}

export interface ProjectedMonth {
  month: string; // YYYY-MM
  totalGross: number;
  totalNet: number;
  installments: InstallmentWithSale[];
}

export interface InstallmentFilters {
  status?: InstallmentStatus;
  saleId?: string;
  startDue?: string;
  endDue?: string;
}

// ── Canais de venda ─────────────────────────────────────────────────────────

export interface SaleChannel {
  id: string;
  name: string;
  active: boolean;
}
