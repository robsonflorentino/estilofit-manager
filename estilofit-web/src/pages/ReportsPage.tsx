import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  BarChart2, DollarSign, ShoppingBag, TrendingUp, Wallet, Loader2, Target, Settings as SettingsIcon,
} from "lucide-react";
import {
  ResponsiveContainer,
  LineChart, Line,
  BarChart, Bar,
  ComposedChart,
  PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, CartesianGrid, Legend,
} from "recharts";
import type { LucideIcon } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { reportService } from "../services/reportService";
import type { ReportPeriod } from "../types/report";
import { PAYMENT_METHOD_LABELS } from "../types/sale";
import type { PaymentMethod } from "../types/sale";

const money = (n: number | null | undefined) =>
  n == null ? "R$ —" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const shortMoney = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { notation: "compact", style: "currency", currency: "BRL" });

// Formatters do Recharts recebem valores potencialmente indefinidos; normalizamos aqui.
const moneyTip = (v: unknown) => money(v == null ? null : Number(v));

const dayLabel = (iso: string) => {
  const [, m, d] = iso.split("-");
  return `${d}/${m}`;
};

const paymentLabel = (label: string) =>
  PAYMENT_METHOD_LABELS[label as PaymentMethod] ?? label;

const monthLabel = (ym: string) => {
  const [y, m] = ym.split("-").map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString("pt-BR", { month: "short", year: "2-digit" });
};

const PIE_COLORS = ["#7c3aed", "#a78bfa", "#c4b5fd", "#8b5cf6", "#6d28d9", "#ddd6fe"];

function firstDayOfMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
}
function todayISO(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

interface KpiCardProps {
  icon: LucideIcon;
  value: string;
  label: string;
}
function KpiCard({ icon: Icon, value, label }: KpiCardProps) {
  return (
    <div className="card border-t-[3px] border-t-brand-purple">
      <Icon className="mb-3 h-6 w-6 text-brand-purple" />
      <p className="text-xl font-bold text-content-primary">{value}</p>
      <p className="mt-1 text-sm text-content-secondary">{label}</p>
    </div>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="card">
      <h3 className="mb-4 font-semibold text-content-primary">{title}</h3>
      {children}
    </div>
  );
}

export function ReportsPage() {
  const [startDate, setStartDate] = useState(firstDayOfMonth());
  const [endDate, setEndDate] = useState(todayISO());

  const period: ReportPeriod = { startDate, endDate };
  const key = [startDate, endDate];

  const [targetMonths, setTargetMonths] = useState(6);

  const summary = useQuery({ queryKey: ["report", "summary", ...key], queryFn: () => reportService.summary(period) });
  const byDay = useQuery({ queryKey: ["report", "by-day", ...key], queryFn: () => reportService.revenueByDay(period) });
  const top = useQuery({ queryKey: ["report", "top", ...key], queryFn: () => reportService.topProducts(period, 10) });
  const byChannel = useQuery({ queryKey: ["report", "channel", ...key], queryFn: () => reportService.byChannel(period) });
  const byPayment = useQuery({ queryKey: ["report", "payment", ...key], queryFn: () => reportService.byPayment(period) });
  const salesTarget = useQuery({ queryKey: ["report", "sales-target", targetMonths], queryFn: () => reportService.salesTarget(targetMonths) });

  const dayData = (byDay.data ?? []).map((d) => ({ ...d, label: dayLabel(d.day) }));
  const topData = (top.data ?? []).map((t) => ({
    ...t,
    label: `${t.productName} ${t.size}/${t.color}`,
  }));
  const channelData = byChannel.data ?? [];
  const paymentData = (byPayment.data ?? []).map((p) => ({ ...p, label: paymentLabel(p.label) }));
  const targetData = (salesTarget.data?.months ?? []).map((m) => ({ ...m, label: monthLabel(m.month) }));
  const currentTarget = salesTarget.data?.months[salesTarget.data.months.length - 1];

  return (
    <div>
      <PageHeader
        icon={BarChart2}
        title="Relatórios"
        description="Indicadores de vendas por período. Selecione o intervalo desejado."
      />

      {/* Seletor de período */}
      <div className="mb-6 flex flex-wrap items-end gap-3">
        <div>
          <label className="mb-1.5 block text-sm text-content-secondary">De</label>
          <input type="date" className="input-base" value={startDate} max={endDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div>
          <label className="mb-1.5 block text-sm text-content-secondary">Até</label>
          <input type="date" className="input-base" value={endDate} min={startDate} max={todayISO()} onChange={(e) => setEndDate(e.target.value)} />
        </div>
      </div>

      {/* Meta de vendas para o pró-labore */}
      <div className="mb-6">
        <div className="card">
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="flex items-center gap-2 font-semibold text-content-primary">
                <Target className="h-5 w-5 text-brand-purple" /> Meta de vendas para o pró-labore
              </h3>
              <p className="mt-1 text-sm text-content-secondary">
                Quanto é preciso faturar por mês para retirar o pró-labore desejado
                {salesTarget.data && (
                  <> de <span className="font-medium text-content-primary">{money(salesTarget.data.targetProLabore)}</span> ({salesTarget.data.proLaborePct}% do lucro)</>
                )}.
                {" "}
                <Link to="/settings" className="inline-flex items-center gap-1 text-brand-purple hover:underline">
                  <SettingsIcon className="h-3.5 w-3.5" /> Ajustar
                </Link>
              </p>
            </div>
            <select className="input-base w-32" value={targetMonths} onChange={(e) => setTargetMonths(Number(e.target.value))}>
              {[6, 12].map((n) => <option key={n} value={n}>{n} meses</option>)}
            </select>
          </div>

          {/* Resumo do mês corrente */}
          {currentTarget && (
            <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="rounded-card bg-bg-input p-3">
                <p className="text-xs text-content-muted">Meta deste mês</p>
                <p className="text-lg font-bold text-content-primary">{money(currentTarget.target)}</p>
              </div>
              <div className="rounded-card bg-bg-input p-3">
                <p className="text-xs text-content-muted">Realizado</p>
                <p className="text-lg font-bold text-content-primary">{money(currentTarget.revenue)}</p>
              </div>
              <div className="rounded-card bg-bg-input p-3">
                <p className="text-xs text-content-muted">Situação</p>
                <p className={`text-lg font-bold ${currentTarget.achieved ? "text-state-success" : "text-state-warning"}`}>
                  {currentTarget.achieved ? "Meta atingida" : "Abaixo da meta"}
                </p>
              </div>
            </div>
          )}

          {salesTarget.isLoading ? (
            <div className="flex h-72 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <ComposedChart data={targetData} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" />
                <XAxis dataKey="label" stroke="#9ca3af" fontSize={12} />
                <YAxis stroke="#9ca3af" fontSize={12} tickFormatter={(v) => shortMoney(Number(v))} />
                <Tooltip
                  formatter={moneyTip}
                  contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }}
                  labelStyle={{ color: "#e5e7eb" }}
                />
                <Legend />
                <Bar dataKey="revenue" name="Realizado" fill="#7c3aed" radius={[4, 4, 0, 0]} />
                <Line type="monotone" dataKey="target" name="Meta" stroke="#f59e0b" strokeWidth={2} dot={{ r: 3 }} />
              </ComposedChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* KPIs do resumo */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard icon={DollarSign} value={money(summary.data?.revenue)} label="Faturamento" />
        <KpiCard icon={ShoppingBag} value={summary.data ? summary.data.saleCount.toLocaleString("pt-BR") : "—"} label="Nº de vendas" />
        <KpiCard icon={TrendingUp} value={money(summary.data?.averageTicket)} label="Ticket médio" />
        <KpiCard icon={Wallet} value={money(summary.data?.estimatedProfit)} label="Lucro estimado" />
      </div>

      {/* Faturamento por dia */}
      <div className="mb-6">
        <ChartCard title="Faturamento por dia">
          {byDay.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : dayData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={dayData} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" />
                <XAxis dataKey="label" stroke="#9ca3af" fontSize={12} />
                <YAxis stroke="#9ca3af" fontSize={12} tickFormatter={(v) => shortMoney(Number(v))} />
                <Tooltip
                  formatter={moneyTip}
                  contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }}
                  labelStyle={{ color: "#e5e7eb" }}
                />
                <Line type="monotone" dataKey="revenue" name="Faturamento" stroke="#7c3aed" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* Top produtos */}
      <div className="mb-6">
        <ChartCard title="Produtos mais vendidos (por quantidade)">
          {top.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : topData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <ResponsiveContainer width="100%" height={Math.max(220, topData.length * 42)}>
              <BarChart data={topData} layout="vertical" margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" horizontal={false} />
                <XAxis type="number" stroke="#9ca3af" fontSize={12} allowDecimals={false} />
                <YAxis type="category" dataKey="label" stroke="#9ca3af" fontSize={11} width={180} />
                <Tooltip
                  formatter={(v: unknown) => [`${Number(v)} un`, "Quantidade"]}
                  contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }}
                  labelStyle={{ color: "#e5e7eb" }}
                />
                <Bar dataKey="quantity" name="Quantidade" fill="#7c3aed" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* Por canal e por pagamento (pizza) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ChartCard title="Vendas por canal">
          {byChannel.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : channelData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={channelData} dataKey="revenue" nameKey="label" cx="50%" cy="50%" outerRadius={90}
                  label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}>
                  {channelData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={moneyTip} contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </ChartCard>

        <ChartCard title="Vendas por forma de pagamento">
          {byPayment.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : paymentData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={paymentData} dataKey="revenue" nameKey="label" cx="50%" cy="50%" outerRadius={90}
                  label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}>
                  {paymentData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={moneyTip} contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>
    </div>
  );
}
