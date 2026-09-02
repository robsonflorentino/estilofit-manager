import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Tag, TrendingDown, PackageX } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { promotionService } from "../services/promotionService";
import type { StaleProduct } from "../types/promotion";

const money = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const dateOnly = (iso: string | null | undefined) =>
  iso == null ? null : new Date(iso).toLocaleDateString("pt-BR");

export function PromotionsPage() {
  // vazio = usa o padrão do backend (PROMOTION_ALERT_DAYS)
  const [daysInput, setDaysInput] = useState("");

  const days = daysInput.trim() === "" ? undefined : Number(daysInput);

  const { data, isLoading } = useQuery({
    queryKey: ["promotions", "stale", days ?? "default"],
    queryFn: () => promotionService.stale(days),
  });

  const staleBadge = (d: StaleProduct) => {
    const variant = d.daysStale >= 90 ? "danger" : "warning";
    return <Badge variant={variant}>{d.daysStale} dias</Badge>;
  };

  const columns: Column<StaleProduct>[] = [
    { header: "SKU", render: (d) => <span className="font-mono text-xs text-brand-purple">{d.sku}</span> },
    { header: "Produto", render: (d) => <span className="font-medium">{d.productName}</span> },
    { header: "Tam/Cor", render: (d) => <span className="text-content-secondary">{d.size} / {d.color}</span> },
    { header: "Estoque", render: (d) => d.stockQuantity },
    { header: "Preço", render: (d) => money(d.salePrice) },
    { header: "Capital parado", render: (d) => <span className="text-content-secondary">{money(d.stockValue)}</span> },
    {
      header: "Última venda",
      render: (d) =>
        d.neverSold ? (
          <span className="text-content-muted">Nunca vendeu</span>
        ) : (
          <span className="text-content-secondary">{dateOnly(d.lastSaleAt)}</span>
        ),
    },
    { header: "Parada há", render: staleBadge },
  ];

  return (
    <div>
      <PageHeader
        icon={Tag}
        title="Alertas de Promoção"
        description="Variações com estoque paradas há muito tempo. Considere uma promoção para girar o estoque."
      />

      {/* Filtro + resumo */}
      <div className="mb-4 flex flex-wrap items-end gap-4">
        <div>
          <label className="mb-1.5 block text-sm text-content-secondary">Sem venda há (dias)</label>
          <input
            type="number"
            min="0"
            className="input-base w-40"
            placeholder={data ? `Padrão: ${data.thresholdDays}` : "Padrão"}
            value={daysInput}
            onChange={(e) => setDaysInput(e.target.value)}
          />
        </div>
      </div>

      {/* Cards de resumo */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="card border-t-[3px] border-t-brand-purple">
          <PackageX className="mb-3 h-6 w-6 text-brand-purple" />
          <p className="text-xl font-bold text-content-primary">{data ? data.staleCount : "—"}</p>
          <p className="mt-1 text-sm text-content-secondary">Variações paradas</p>
        </div>
        <div className="card border-t-[3px] border-t-state-warning">
          <TrendingDown className="mb-3 h-6 w-6 text-state-warning" />
          <p className="text-xl font-bold text-content-primary">{money(data?.totalStockValue)}</p>
          <p className="mt-1 text-sm text-content-secondary">Capital parado</p>
        </div>
        <div className="card border-t-[3px] border-t-border">
          <Tag className="mb-3 h-6 w-6 text-content-secondary" />
          <p className="text-xl font-bold text-content-primary">{data ? `${data.thresholdDays} dias` : "—"}</p>
          <p className="mt-1 text-sm text-content-secondary">Limiar aplicado</p>
        </div>
      </div>

      <DataTable
        columns={columns}
        rows={data?.items ?? []}
        rowKey={(d) => d.variantId}
        loading={isLoading}
        emptyMessage="Nenhuma variação parada no período. Bom giro de estoque!"
      />
    </div>
  );
}
