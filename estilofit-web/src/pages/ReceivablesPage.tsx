import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { CircleDollarSign, CheckCircle2, Loader2, TrendingUp } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Pagination } from "../components/Pagination";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { saleService } from "../services/saleService";
import { getApiErrorMessage } from "../lib/api";
import type { InstallmentStatus, InstallmentWithSale } from "../types/sale";
import { INSTALLMENT_STATUS_LABELS } from "../types/sale";

const PAGE_SIZE = 15;

const money = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const dateOnly = (iso: string | null | undefined) =>
  iso == null ? "—" : new Date(`${iso}T00:00:00`).toLocaleDateString("pt-BR");

const monthLabel = (ym: string) => {
  const [y, m] = ym.split("-").map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
};

const STATUSES: InstallmentStatus[] = ["PENDING", "RECEIVED", "CANCELLED"];

type Tab = "list" | "projected";

export function ReceivablesPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>("list");

  // ── Aba lista ────────────────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<InstallmentStatus | "">("PENDING");
  const [receiving, setReceiving] = useState<InstallmentWithSale | null>(null);

  const filters = { status: status || undefined };

  const { data, isLoading } = useQuery({
    queryKey: ["installments", page, filters],
    queryFn: () => saleService.listInstallments(page, PAGE_SIZE, filters),
  });

  const receiveMutation = useMutation({
    mutationFn: () => saleService.receiveInstallment(receiving!.id),
    onSuccess: () => {
      toast.success("Baixa registrada.");
      setReceiving(null);
      queryClient.invalidateQueries({ queryKey: ["installments"] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível dar baixa na parcela.")),
  });

  // ── Aba projetado ────────────────────────────────────────────────────
  const [months, setMonths] = useState(6);
  const { data: projected, isLoading: projLoading } = useQuery({
    queryKey: ["installments", "projected", months],
    queryFn: () => saleService.projected(months),
    enabled: tab === "projected",
  });

  const statusBadge = (s: InstallmentStatus) => {
    const variant = s === "RECEIVED" ? "success" : s === "PENDING" ? "warning" : "danger";
    return <Badge variant={variant}>{INSTALLMENT_STATUS_LABELS[s]}</Badge>;
  };

  const columns: Column<InstallmentWithSale>[] = [
    { header: "Vencimento", render: (i) => dateOnly(i.dueDate) },
    { header: "Venda", render: (i) => <span className="font-mono text-xs text-brand-purple">{i.sale.id.slice(0, 8)}</span> },
    { header: "Parcela", render: (i) => `${i.installmentNum}` },
    { header: "Bruto", render: (i) => money(i.grossAmount) },
    { header: "Líquido", render: (i) => <span className="font-medium">{money(i.netAmount)}</span> },
    { header: "Status", render: (i) => statusBadge(i.status) },
    {
      header: "Ações",
      className: "text-right",
      render: (i) =>
        i.status === "PENDING" ? (
          <button
            onClick={() => setReceiving(i)}
            className="inline-flex items-center gap-1 rounded-btn px-2 py-1.5 text-content-secondary hover:bg-bg-surface-raised hover:text-state-success"
            title="Dar baixa"
          >
            <CheckCircle2 className="h-4 w-4" /> Baixar
          </button>
        ) : (
          <span className="text-content-muted">{i.receivedAt ? `Em ${dateOnly(i.receivedAt.slice(0, 10))}` : "—"}</span>
        ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={CircleDollarSign}
        title="Contas a Receber"
        description="Parcelas das vendas no cartão de crédito e o fluxo de caixa projetado."
      />

      {/* Abas */}
      <div className="mb-4 flex gap-1 border-b border-border-subtle">
        <button
          onClick={() => setTab("list")}
          className={`px-4 py-2 text-sm font-medium ${tab === "list" ? "border-b-2 border-brand-purple text-brand-purple" : "text-content-secondary hover:text-content-primary"}`}
        >
          Parcelas
        </button>
        <button
          onClick={() => setTab("projected")}
          className={`inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium ${tab === "projected" ? "border-b-2 border-brand-purple text-brand-purple" : "text-content-secondary hover:text-content-primary"}`}
        >
          <TrendingUp className="h-4 w-4" /> Fluxo projetado
        </button>
      </div>

      {tab === "list" ? (
        <>
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <select className="input-base w-48" value={status} onChange={(e) => { setStatus(e.target.value as InstallmentStatus | ""); setPage(0); }}>
              <option value="">Todos os status</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>{INSTALLMENT_STATUS_LABELS[s]}</option>
              ))}
            </select>
          </div>

          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            rowKey={(i) => i.id}
            loading={isLoading}
            emptyMessage="Nenhuma parcela encontrada."
          />

          {data && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={data.size}
              onPageChange={setPage}
            />
          )}
        </>
      ) : (
        <>
          <div className="mb-4 flex items-center gap-3">
            <label className="text-sm text-content-secondary">Projetar próximos</label>
            <select className="input-base w-32" value={months} onChange={(e) => setMonths(Number(e.target.value))}>
              {[3, 6, 12].map((n) => (
                <option key={n} value={n}>{n} meses</option>
              ))}
            </select>
          </div>

          {projLoading ? (
            <div className="py-10 text-center"><Loader2 className="mx-auto h-6 w-6 animate-spin text-brand-purple" /></div>
          ) : !projected || projected.length === 0 ? (
            <div className="card text-center text-content-muted">Nenhuma parcela pendente no período.</div>
          ) : (
            <div className="space-y-4">
              {projected.map((m) => (
                <div key={m.month} className="card">
                  <div className="mb-3 flex items-center justify-between">
                    <h3 className="font-semibold capitalize text-content-primary">{monthLabel(m.month)}</h3>
                    <div className="text-right text-sm">
                      <div className="text-content-secondary">Bruto: {money(m.totalGross)}</div>
                      <div className="font-semibold text-state-success">Líquido: {money(m.totalNet)}</div>
                    </div>
                  </div>
                  <div className="overflow-hidden rounded-card border border-border-subtle">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-bg-surface-raised text-content-secondary">
                        <tr><th className="px-2 py-1.5">Vencimento</th><th className="px-2 py-1.5">Venda</th><th className="px-2 py-1.5">Parc.</th><th className="px-2 py-1.5">Bruto</th><th className="px-2 py-1.5">Líquido</th></tr>
                      </thead>
                      <tbody className="divide-y divide-border-subtle">
                        {m.installments.map((i) => (
                          <tr key={i.id}>
                            <td className="px-2 py-1.5">{dateOnly(i.dueDate)}</td>
                            <td className="px-2 py-1.5 font-mono text-brand-purple">{i.sale.id.slice(0, 8)}</td>
                            <td className="px-2 py-1.5">{i.installmentNum}</td>
                            <td className="px-2 py-1.5">{money(i.grossAmount)}</td>
                            <td className="px-2 py-1.5">{money(i.netAmount)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={receiving !== null}
        title="Dar baixa na parcela"
        message={
          receiving
            ? `Confirmar recebimento da parcela ${receiving.installmentNum} (${money(receiving.netAmount)} líquido) com vencimento em ${dateOnly(receiving.dueDate)}?`
            : ""
        }
        confirmLabel="Confirmar baixa"
        loading={receiveMutation.isPending}
        onConfirm={() => receiveMutation.mutate()}
        onCancel={() => setReceiving(null)}
      />
    </div>
  );
}
