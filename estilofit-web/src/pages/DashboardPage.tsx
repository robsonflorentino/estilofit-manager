import { useQuery } from "@tanstack/react-query";
import { DollarSign, ShoppingBag, Package, Wallet } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "../store/authStore";
import { dashboardService } from "../services/dashboardService";

const money = (n: number | null | undefined) =>
  n == null ? "R$ —" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const count = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR");

interface KpiCardProps {
  icon: LucideIcon;
  value: string;
  label: string;
  loading?: boolean;
}

function KpiCard({ icon: Icon, value, label, loading = false }: KpiCardProps) {
  return (
    <div className="card border-t-[3px] border-t-brand-purple">
      <Icon className="mb-3 h-7 w-7 text-brand-purple" />
      {loading ? (
        <div className="h-8 w-24 animate-pulse rounded bg-bg-surface-raised" />
      ) : (
        <p className="text-2xl font-bold text-content-primary">{value}</p>
      )}
      <p className="mt-1 text-sm text-content-secondary">{label}</p>
    </div>
  );
}

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", "kpis"],
    queryFn: () => dashboardService.getKpis(),
  });

  // O backend só envia estoque/pró-labore para perfis de gestão (nulos para vendedor)
  const showManagement = data?.stockItems != null || data?.estimatedProLabore != null;

  return (
    <div>
      {/* Cabeçalho */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-content-primary">Dashboard</h1>
        <p className="mt-1 text-sm text-content-secondary">
          Olá, {user?.name}. Aqui está a visão geral da loja no mês.
        </p>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          icon={DollarSign}
          value={money(data?.monthRevenue)}
          label="Vendas do mês"
          loading={isLoading}
        />
        <KpiCard
          icon={ShoppingBag}
          value={count(data?.saleCount)}
          label="Nº de vendas"
          loading={isLoading}
        />
        {(isLoading || showManagement) && (
          <>
            <KpiCard
              icon={Package}
              value={count(data?.stockItems)}
              label="Itens em estoque"
              loading={isLoading}
            />
            <KpiCard
              icon={Wallet}
              value={money(data?.estimatedProLabore)}
              label="Pró-labore estimado"
              loading={isLoading}
            />
          </>
        )}
      </div>

      {!isLoading && !showManagement && (
        <div className="card mt-6">
          <p className="text-sm text-content-secondary">
            Estes são os seus indicadores de vendas do mês. Indicadores de estoque e
            pró-labore ficam disponíveis para perfis de gestão.
          </p>
        </div>
      )}
    </div>
  );
}
