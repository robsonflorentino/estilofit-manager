export interface Supplier {
  id: string;
  name: string;
  contactPhone: string | null;
  contactEmail: string | null;
  whatsapp: string | null;
  cnpj: string | null;
  address: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
}

export interface SupplierRequest {
  name: string;
  contactPhone?: string;
  contactEmail?: string;
  whatsapp?: string;
  cnpj?: string;
  address?: string;
  notes?: string;
}

export interface SupplierFilters {
  name?: string;
  active?: boolean;
}
