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
import { Badge } from "../components/Badge";
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

// Estilos do pódio (ouro, prata, bronze) — index 0/1/2
const MEDALS = [
  { emoji: "🥇", label: "Ouro", ring: "border-t-[#facc15]", badge: "bg-[#facc15]/15 text-[#facc15]" },
  { emoji: "🥈", label: "Prata", ring: "border-t-[#cbd5e1]", badge: "bg-[#cbd5e1]/15 text-[#cbd5e1]" },
  { emoji: "🥉", label: "Bronze", ring: "border-t-[#d97706]", badge: "bg-[#d97706]/15 text-[#d97706]" },
];

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
  const profitByChannel = useQuery({ queryKey: ["report", "profit-channel", ...key], queryFn: () => reportService.profitByChannel(period) });
  const sellerRanking = useQuery({ queryKey: ["report", "seller-ranking", ...key], queryFn: () => reportService.sellerRanking(period) });
  const [refDays, setRefDays] = useState(30);
  const purchaseSuggestion = useQuery({ queryKey: ["report", "purchase-suggestion", refDays], queryFn: () => reportService.purchaseSuggestion(refDays) });

  const dayData = (byDay.data ?? []).map((d) => ({ ...d, label: dayLabel(d.day) }));
  const topData = (top.data ?? []).map((t) => ({
    ...t,
    label: `${t.productName} ${t.size}/${t.color}`,
  }));
  const channelData = byChannel.data ?? [];
  const paymentData = (byPayment.data ?? []).map((p) => ({ ...p, label: paymentLabel(p.label) }));
  const targetData = (salesTarget.data?.months ?? []).map((m) => ({ ...m, label: monthLabel(m.month) }));
  const currentTarget = salesTarget.data?.months[salesTarget.data.months.length - 1];
  const profitData = profitByChannel.data ?? [];
  const rankingData = sellerRanking.data ?? [];
  const podium = rankingData.slice(0, 3);
  const rest = rankingData.slice(3);
  const purchaseData = purchaseSuggestion.data;

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

      {/* Lucratividade por canal (detalhado) */}
      <div className="mt-6">
        <ChartCard title="Lucratividade por canal">
          {profitByChannel.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : profitData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <div className="space-y-6">
              <ResponsiveContainer width="100%" height={Math.max(200, profitData.length * 48)}>
                <BarChart data={profitData} layout="vertical" margin={{ top: 8, right: 24, bottom: 8, left: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#2a2a3a" horizontal={false} />
                  <XAxis type="number" stroke="#9ca3af" fontSize={12} tickFormatter={(v) => shortMoney(Number(v))} />
                  <YAxis type="category" dataKey="channel" stroke="#9ca3af" fontSize={12} width={110} />
                  <Tooltip
                    formatter={moneyTip}
                    contentStyle={{ background: "#1a1a24", border: "1px solid #2a2a3a", borderRadius: 8 }}
                    labelStyle={{ color: "#e5e7eb" }}
                  />
                  <Bar dataKey="profit" name="Lucro" fill="#7c3aed" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>

              <div className="overflow-hidden rounded-card border border-border-subtle">
                <table className="w-full text-left text-sm">
                  <thead className="bg-bg-surface-raised text-xs uppercase tracking-wider text-content-secondary">
                    <tr>
                      <th className="px-3 py-2">Canal</th>
                      <th className="px-3 py-2">Faturamento</th>
                      <th className="px-3 py-2">Custo</th>
                      <th className="px-3 py-2">Lucro</th>
                      <th className="px-3 py-2">Margem</th>
                      <th className="px-3 py-2">Vendas</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border-subtle">
                    {profitData.map((c) => (
                      <tr key={c.channel} className="bg-bg-surface">
                        <td className="px-3 py-2 font-medium text-content-primary">{c.channel}</td>
                        <td className="px-3 py-2 text-content-secondary">{money(c.revenue)}</td>
                        <td className="px-3 py-2 text-content-secondary">{money(c.cost)}</td>
                        <td className="px-3 py-2 font-medium text-content-primary">{money(c.profit)}</td>
                        <td className="px-3 py-2 text-content-secondary">{c.marginPct}%</td>
                        <td className="px-3 py-2 text-content-secondary">{c.saleCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </ChartCard>
      </div>

      {/* Ranking de vendedores */}
      <div className="mt-6">
        <ChartCard title="Ranking de vendedores">
          {sellerRanking.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : rankingData.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Sem vendas no período.</p>
          ) : (
            <div className="space-y-6">
              {/* Pódio (top 3) */}
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                {podium.map((seller, i) => (
                  <div key={seller.sellerId} className={`card border-t-[3px] ${MEDALS[i].ring}`}>
                    <div className="flex items-center justify-between">
                      <span className="text-2xl">{MEDALS[i].emoji}</span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${MEDALS[i].badge}`}>
                        {MEDALS[i].label}
                      </span>
                    </div>
                    <p className="mt-2 truncate font-semibold text-content-primary" title={seller.sellerName}>
                      {seller.sellerName}
                    </p>
                    <p className="text-lg font-bold text-content-primary">{money(seller.revenue)}</p>
                    <p className="text-xs text-content-muted">
                      {seller.saleCount} venda(s) · ticket {money(seller.averageTicket)}
                    </p>
                  </div>
                ))}
              </div>

              {/* Demais posições */}
              {rest.length > 0 && (
                <div className="overflow-hidden rounded-card border border-border-subtle">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-bg-surface-raised text-xs uppercase tracking-wider text-content-secondary">
                      <tr>
                        <th className="px-3 py-2">#</th>
                        <th className="px-3 py-2">Vendedor</th>
                        <th className="px-3 py-2">Faturamento</th>
                        <th className="px-3 py-2">Vendas</th>
                        <th className="px-3 py-2">Ticket médio</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border-subtle">
                      {rest.map((seller) => (
                        <tr key={seller.sellerId} className="bg-bg-surface">
                          <td className="px-3 py-2 text-content-secondary">{seller.position}º</td>
                          <td className="px-3 py-2 font-medium text-content-primary">{seller.sellerName}</td>
                          <td className="px-3 py-2 text-content-secondary">{money(seller.revenue)}</td>
                          <td className="px-3 py-2 text-content-secondary">{seller.saleCount}</td>
                          <td className="px-3 py-2 text-content-secondary">{money(seller.averageTicket)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </ChartCard>
      </div>

      {/* Sugestão de compra do próximo lote */}
      <div className="mt-6">
        <ChartCard title="Sugestão de compra do próximo lote">
          <div className="mb-4 flex flex-wrap items-center gap-3 text-sm text-content-secondary">
            <span>Baseado nas vendas dos últimos</span>
            <select className="input-base w-28" value={refDays} onChange={(e) => setRefDays(Number(e.target.value))}>
              {[30, 60, 90].map((n) => <option key={n} value={n}>{n} dias</option>)}
            </select>
            {purchaseData && (
              <span>· para cobrir os próximos <span className="text-content-primary">{purchaseData.coverageTargetDays} dias</span></span>
            )}
          </div>

          {purchaseSuggestion.isLoading ? (
            <div className="flex h-64 items-center justify-center"><Loader2 className="h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : !purchaseData || purchaseData.groups.length === 0 ? (
            <p className="py-16 text-center text-sm text-content-muted">Nenhum item precisa de reposição no momento. Estoque saudável!</p>
          ) : (
            <>
              <div className="mb-4 flex flex-wrap gap-6 text-sm">
                <div>
                  <span className="text-content-muted">Itens a comprar: </span>
                  <span className="font-semibold text-content-primary">{purchaseData.totalItems}</span>
                </div>
                <div>
                  <span className="text-content-muted">Custo estimado do lote: </span>
                  <span className="font-semibold text-content-primary">{money(purchaseData.totalEstimatedCost)}</span>
                </div>
              </div>

              <div className="space-y-5">
                {purchaseData.groups.map((g) => (
                  <div key={g.supplierId ?? "none"} className="overflow-hidden rounded-card border border-border-subtle">
                    {/* Cabeçalho do fornecedor com subtotal */}
                    <div className="flex flex-wrap items-center justify-between gap-2 bg-bg-surface-raised px-3 py-2">
                      <span className="font-semibold text-content-primary">{g.supplierName}</span>
                      <span className="text-sm text-content-secondary">
                        {g.itemCount} {g.itemCount === 1 ? "item" : "itens"} · <span className="font-medium text-content-primary">{money(g.estimatedCost)}</span>
                      </span>
                    </div>
                    <table className="w-full text-left text-sm">
                      <thead className="bg-bg-surface text-xs uppercase tracking-wider text-content-muted">
                        <tr>
                          <th className="px-3 py-2">Produto</th>
                          <th className="px-3 py-2">Estoque</th>
                          <th className="px-3 py-2">Vendas ({purchaseData.referenceDays}d)</th>
                          <th className="px-3 py-2">Venda/dia</th>
                          <th className="px-3 py-2">Cobertura</th>
                          <th className="px-3 py-2">Sugestão</th>
                          <th className="px-3 py-2">Custo estimado</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border-subtle">
                        {g.items.map((p) => (
                          <tr key={p.variantId} className="bg-bg-surface">
                            <td className="px-3 py-2">
                              <span className="font-mono text-xs text-brand-purple">{p.sku}</span>
                              <span className="ml-2 text-content-secondary">{p.productName} · {p.size}/{p.color}</span>
                            </td>
                            <td className="px-3 py-2">
                              {p.belowMinimum ? (
                                <Badge variant="danger">{p.stockQuantity}</Badge>
                              ) : (
                                <span className="text-content-secondary">{p.stockQuantity}</span>
                              )}
                            </td>
                            <td className="px-3 py-2 text-content-secondary">{p.soldQty}</td>
                            <td className="px-3 py-2 text-content-secondary">{p.dailyVelocity}</td>
                            <td className="px-3 py-2">
                              {p.coverageDays == null ? (
                                <span className="text-content-muted">—</span>
                              ) : p.coverageDays <= purchaseData.coverageTargetDays ? (
                                <Badge variant="warning">{p.coverageDays} dias</Badge>
                              ) : (
                                <span className="text-content-secondary">{p.coverageDays} dias</span>
                              )}
                            </td>
                            <td className="px-3 py-2 font-semibold text-content-primary">{p.suggestedQty} un</td>
                            <td className="px-3 py-2 text-content-secondary">{money(p.estimatedCost)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ))}
              </div>
            </>
          )}
        </ChartCard>
      </div>
    </div>
  );
}
