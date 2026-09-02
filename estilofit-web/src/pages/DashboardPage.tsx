import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { DollarSign, ShoppingBag, Package, Wallet, Target, Store } from "lucide-react";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
} from "recharts";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "../store/authStore";
import { dashboardService } from "../services/dashboardService";
import { reportService } from "../services/reportService";

const shortMoney = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { notation: "compact", style: "currency", currency: "BRL" });

const moneyTip = (v: unknown) =>
  v == null ? "R$ —" : Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

function firstDayOfMonthISO(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
}
function todayISO(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

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

/** Card compacto de lucro por canal no mês corrente (só gestão). */
function ChannelProfitCard() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", "profit-by-channel"],
    queryFn: () =>
      reportService.profitByChannel({ startDate: firstDayOfMonthISO(), endDate: todayISO() }),
  });

  const top = data?.[0]; // já vem ordenado por lucro desc

  return (
    <div className="card mt-6">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-2">
        <h2 className="flex items-center gap-2 font-semibold text-content-primary">
          <Store className="h-5 w-5 text-brand-purple" /> Lucro por canal (mês)
        </h2>
        <Link to="/reports" className="text-xs text-brand-purple hover:underline">ver detalhado</Link>
      </div>

      {isLoading ? (
        <div className="h-40 w-full animate-pulse rounded bg-bg-surface-raised" />
      ) : !data || data.length === 0 ? (
        <p className="py-8 text-center text-sm text-content-muted">Sem vendas no mês ainda.</p>
      ) : (
        <>
          {top && (
            <p className="mb-3 text-sm text-content-secondary">
              Canal mais lucrativo:{" "}
              <span className="font-semibold text-content-primary">{top.channel}</span>{" "}
              ({money(top.profit)})
            </p>
          )}
          <ResponsiveContainer width="100%" height={Math.max(160, data.length * 44)}>
            <BarChart data={data} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" horizontal={false} />
              <XAxis type="number" stroke="#9ca3af" fontSize={12} tickFormatter={(v) => shortMoney(Number(v))} />
              <YAxis type="category" dataKey="channel" stroke="#9ca3af" fontSize={12} width={100} />
              <Tooltip
                formatter={moneyTip}
                contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }}
                labelStyle={{ color: "#e5e7eb" }}
              />
              <Bar dataKey="profit" name="Lucro" fill="#7c3aed" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
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

      {/* Meta do mês e lucro por canal — apenas gestão */}
      {isManagement && <MonthTargetCard />}
      {isManagement && <ChannelProfitCard />}

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
