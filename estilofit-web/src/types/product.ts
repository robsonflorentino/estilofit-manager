export interface CategorySummary {
  id: string;
  name: string;
}

export interface Variant {
  id: string;
  sku: string;
  size: string;
  color: string;
  profitMargin: number | null;
  salePrice: number | null;
  averageCost: number | null;
  stockQuantity: number;
  active: boolean;
}

export interface ProductSummary {
  id: string;
  name: string;
  category: CategorySummary;
  active: boolean;
  variantCount: number;
  totalStock: number;
  createdAt: string;
}

export interface ProductDetail {
  id: string;
  name: string;
  description: string | null;
  category: { id: string; name: string; active: boolean };
  active: boolean;
  variants: Variant[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  name: string;
  description?: string;
  categoryId: string;
}

export interface CreateVariantRequest {
  size: string;
  color: string;
  profitMargin?: number | null;
}

export interface UpdateVariantRequest {
  profitMargin?: number | null;
  salePrice?: number | null;
}

export interface ProductFilters {
  name?: string;
  categoryId?: string;
  active?: boolean;
}
