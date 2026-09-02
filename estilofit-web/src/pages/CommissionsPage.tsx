import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { HandCoins } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { commissionService } from "../services/commissionService";
import type { SellerCommission } from "../types/commission";

const money = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

function firstDayOfMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
}
function todayISO(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export function CommissionsPage() {
  const [startDate, setStartDate] = useState(firstDayOfMonth());
  const [endDate, setEndDate] = useState(todayISO());

  const { data, isLoading } = useQuery({
    queryKey: ["commissions", startDate, endDate],
    queryFn: () => commissionService.report(startDate, endDate),
  });

  const columns: Column<SellerCommission>[] = [
    { header: "Vendedor", render: (s) => <span className="font-medium text-content-primary">{s.sellerName}</span> },
    { header: "Faturamento", render: (s) => <span className="text-content-secondary">{money(s.revenue)}</span> },
    { header: "Vendas", render: (s) => <span className="text-content-secondary">{s.saleCount}</span> },
    { header: "Comissão a pagar", render: (s) => <span className="font-medium text-content-primary">{money(s.commissionAmount)}</span> },
  ];

  return (
    <div>
      <PageHeader
        icon={HandCoins}
        title="Comissões"
        description="Comissões a pagar por vendedor no período. O valor é congelado no momento de cada venda."
      />

      {/* Seletor de período */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label className="mb-1.5 block text-sm text-content-secondary">De</label>
          <input type="date" className="input-base" value={startDate} max={endDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div>
          <label className="mb-1.5 block text-sm text-content-secondary">Até</label>
          <input type="date" className="input-base" value={endDate} min={startDate} max={todayISO()} onChange={(e) => setEndDate(e.target.value)} />
        </div>
      </div>

      {/* Total a pagar */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="card border-t-[3px] border-t-brand-purple">
          <HandCoins className="mb-3 h-6 w-6 text-brand-purple" />
          {isLoading ? (
            <div className="h-7 w-28 animate-pulse rounded bg-bg-surface-raised" />
          ) : (
            <p className="text-xl font-bold text-content-primary">{money(data?.totalCommission)}</p>
          )}
          <p className="mt-1 text-sm text-content-secondary">Total de comissões no período</p>
        </div>
      </div>

      <DataTable
        columns={columns}
        rows={data?.sellers ?? []}
        rowKey={(s) => s.sellerId}
        loading={isLoading}
        emptyMessage="Nenhuma comissão a pagar no período."
      />
    </div>
  );
}
