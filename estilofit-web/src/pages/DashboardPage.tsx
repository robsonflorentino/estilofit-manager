import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { DollarSign, ShoppingBag, Package, Wallet, Target } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "../store/authStore";
import { dashboardService } from "../services/dashboardService";
import { reportService } from "../services/reportService";

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

/** Card enxuto de meta do mês (só gestão): meta, realizado, progresso e situação. */
function MonthTargetCard() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", "month-target"],
    // busca 1 mês; usamos o último item (mês corrente)
    queryFn: () => reportService.salesTarget(1),
  });

  const current = data?.months[data.months.length - 1];

  const progress =
    current && current.target > 0
      ? Math.min((current.revenue / current.target) * 100, 100)
      : 0;
  const remaining = current ? Math.max(current.target - current.revenue, 0) : 0;

  return (
    <div className="card mt-6">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-2">
        <h2 className="flex items-center gap-2 font-semibold text-content-primary">
          <Target className="h-5 w-5 text-brand-purple" /> Meta do mês
        </h2>
        {data && (
          <span className="text-xs text-content-muted">
            para pró-labore de {money(data.targetProLabore)} · {" "}
            <Link to="/reports" className="text-brand-purple hover:underline">ver histórico</Link>
          </span>
        )}
      </div>

      {isLoading || !current ? (
        <div className="h-16 w-full animate-pulse rounded bg-bg-surface-raised" />
      ) : (
        <>
          <div className="mb-3 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <p className="text-xs text-content-muted">Meta de faturamento</p>
              <p className="text-lg font-bold text-content-primary">{money(current.target)}</p>
            </div>
            <div>
              <p className="text-xs text-content-muted">Realizado</p>
              <p className="text-lg font-bold text-content-primary">{money(current.revenue)}</p>
            </div>
            <div>
              <p className="text-xs text-content-muted">Situação</p>
              <p className={`text-lg font-bold ${current.achieved ? "text-state-success" : "text-state-warning"}`}>
                {current.achieved ? "Meta atingida" : `Faltam ${money(remaining)}`}
              </p>
            </div>
          </div>

          {/* Barra de progresso */}
          <div className="h-2.5 w-full overflow-hidden rounded-full bg-bg-input">
            <div
              className={`h-full rounded-full ${current.achieved ? "bg-state-success" : "bg-brand-purple"}`}
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="mt-1 text-right text-xs text-content-muted">{progress.toFixed(0)}% da meta</p>
        </>
      )}
    </div>
  );
}

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const hasRole = useAuthStore((s) => s.hasRole);
  const isManagement = hasRole(["ADMIN", "MANAGER"]);

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

      {/* Meta do mês — apenas gestão */}
      {isManagement && <MonthTargetCard />}

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
