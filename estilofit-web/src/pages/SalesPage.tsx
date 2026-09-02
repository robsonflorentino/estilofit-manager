import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { Receipt, Plus, Eye, Ban, Loader2 } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { RoleGuard } from "../components/RoleGuard";
import { saleService } from "../services/saleService";
import { getApiErrorMessage } from "../lib/api";
import type { PaymentMethod, SaleStatus, SaleSummary } from "../types/sale";
import {
  INSTALLMENT_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  SALE_STATUS_LABELS,
} from "../types/sale";

const PAGE_SIZE = 15;

const money = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const dateTime = (iso: string | null | undefined) =>
  iso == null ? "—" : new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });

const dateOnly = (iso: string | null | undefined) =>
  iso == null ? "—" : new Date(`${iso}T00:00:00`).toLocaleDateString("pt-BR");

const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "PIX", "DEBIT_CARD", "CREDIT_CARD", "TRANSFER"];
const SALE_STATUSES: SaleStatus[] = ["CONFIRMED", "CANCELLED"];

export function SalesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [channelId, setChannelId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | "">("");
  const [status, setStatus] = useState<SaleStatus | "">("");

  const [detailId, setDetailId] = useState<string | null>(null);
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState("");

  const filters = {
    channelId: channelId || undefined,
    paymentMethod: paymentMethod || undefined,
    status: status || undefined,
  };

  const { data, isLoading } = useQuery({
    queryKey: ["sales", page, filters],
    queryFn: () => saleService.list(page, PAGE_SIZE, filters),
  });

  const { data: channels } = useQuery({
    queryKey: ["sale-channels", "active"],
    queryFn: () => saleService.listChannels(false),
  });

  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ["sale", detailId],
    queryFn: () => saleService.getById(detailId!),
    enabled: !!detailId,
  });

  const cancelMutation = useMutation({
    mutationFn: () => saleService.cancel(cancelId!, { reason: cancelReason }),
    onSuccess: () => {
      toast.success("Venda cancelada. Estoque estornado.");
      setCancelId(null);
      setCancelReason("");
      queryClient.invalidateQueries({ queryKey: ["sales"] });
      queryClient.invalidateQueries({ queryKey: ["stock"] });
      queryClient.invalidateQueries({ queryKey: ["installments"] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível cancelar a venda.")),
  });

  const statusBadge = (s: SaleStatus) =>
    s === "CONFIRMED" ? (
      <Badge variant="success">{SALE_STATUS_LABELS[s]}</Badge>
    ) : (
      <Badge variant="danger">{SALE_STATUS_LABELS[s]}</Badge>
    );

  const columns: Column<SaleSummary>[] = [
    { header: "Data", render: (s) => <span className="text-content-secondary">{dateTime(s.confirmedAt)}</span> },
    { header: "Canal", render: (s) => s.channel.name },
    { header: "Vendedor", render: (s) => <span className="text-content-secondary">{s.seller.name}</span> },
    { header: "Pagamento", render: (s) => (
      <span>
        {PAYMENT_METHOD_LABELS[s.paymentMethod]}
        {s.installments > 1 && <span className="text-content-muted"> · {s.installments}x</span>}
      </span>
    ) },
    { header: "Itens", render: (s) => s.itemCount },
    { header: "Total", render: (s) => <span className="font-medium">{money(s.finalAmount)}</span> },
    { header: "Status", render: (s) => statusBadge(s.status) },
    {
      header: "Ações",
      className: "text-right",
      render: (s) => (
        <div className="flex justify-end gap-1">
          <button
            onClick={() => setDetailId(s.id)}
            className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple"
            title="Ver detalhes"
          >
            <Eye className="h-4 w-4" />
          </button>
          {s.status === "CONFIRMED" && (
            <RoleGuard roles={["ADMIN", "MANAGER"]}>
              <button
                onClick={() => { setCancelId(s.id); setCancelReason(""); }}
                className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-state-danger"
                title="Cancelar venda"
              >
                <Ban className="h-4 w-4" />
              </button>
            </RoleGuard>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Receipt}
        title="Vendas"
        description="Histórico de vendas. Vendedores visualizam apenas as próprias."
        action={
          <button className="btn-primary" onClick={() => navigate("/sales/new")}>
            <Plus className="h-4 w-4" />
            Nova venda
          </button>
        }
      />

      {/* Filtros */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className="input-base w-48" value={channelId} onChange={(e) => { setChannelId(e.target.value); setPage(0); }}>
          <option value="">Todos os canais</option>
          {channels?.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <select className="input-base w-48" value={paymentMethod} onChange={(e) => { setPaymentMethod(e.target.value as PaymentMethod | ""); setPage(0); }}>
          <option value="">Todas as formas</option>
          {PAYMENT_METHODS.map((m) => (
            <option key={m} value={m}>{PAYMENT_METHOD_LABELS[m]}</option>
          ))}
        </select>
        <select className="input-base w-40" value={status} onChange={(e) => { setStatus(e.target.value as SaleStatus | ""); setPage(0); }}>
          <option value="">Todos os status</option>
          {SALE_STATUSES.map((s) => (
            <option key={s} value={s}>{SALE_STATUS_LABELS[s]}</option>
          ))}
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(s) => s.id}
        loading={isLoading}
        emptyMessage="Nenhuma venda registrada."
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

      {/* Modal de detalhe */}
      <Modal
        open={detailId !== null}
        title="Detalhes da venda"
        onClose={() => setDetailId(null)}
      >
        {detailLoading || !detail ? (
          <div className="py-8 text-center"><Loader2 className="mx-auto h-6 w-6 animate-spin text-brand-purple" /></div>
        ) : (
          <div className="space-y-4 text-sm">
            <div className="grid grid-cols-2 gap-3">
              <div><span className="text-content-muted">Data</span><div className="text-content-primary">{dateTime(detail.confirmedAt)}</div></div>
              <div><span className="text-content-muted">Status</span><div>{statusBadge(detail.status)}</div></div>
              <div><span className="text-content-muted">Canal</span><div className="text-content-primary">{detail.channel.name}</div></div>
              <div><span className="text-content-muted">Vendedor</span><div className="text-content-primary">{detail.seller.name}</div></div>
              <div><span className="text-content-muted">Pagamento</span><div className="text-content-primary">{PAYMENT_METHOD_LABELS[detail.paymentMethod]}{detail.installments > 1 && ` · ${detail.installments}x`}</div></div>
              {detail.cardFeePct != null && (
                <div><span className="text-content-muted">Taxa maquininha</span><div className="text-content-primary">{detail.cardFeePct}%{detail.cardFeePassed ? " (repassada)" : ""}</div></div>
              )}
            </div>

            {/* Itens */}
            <div>
              <h4 className="mb-1.5 font-medium text-content-primary">Itens</h4>
              <div className="overflow-hidden rounded-card border border-border-subtle">
                <table className="w-full text-left text-xs">
                  <thead className="bg-bg-surface-raised text-content-secondary">
                    <tr><th className="px-2 py-1.5">SKU</th><th className="px-2 py-1.5">Produto</th><th className="px-2 py-1.5">Qtd</th><th className="px-2 py-1.5">Un.</th><th className="px-2 py-1.5">Subtotal</th></tr>
                  </thead>
                  <tbody className="divide-y divide-border-subtle">
                    {detail.items.map((it) => (
                      <tr key={it.id}>
                        <td className="px-2 py-1.5 font-mono text-brand-purple">{it.variant.sku}</td>
                        <td className="px-2 py-1.5">{it.variant.productName} · {it.variant.size}/{it.variant.color}</td>
                        <td className="px-2 py-1.5">{it.quantity}</td>
                        <td className="px-2 py-1.5">{money(it.unitPrice)}</td>
                        <td className="px-2 py-1.5">{money(it.totalPrice)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Totais */}
            <div className="space-y-1 border-t border-border-subtle pt-2">
              <div className="flex justify-between text-content-secondary"><span>Subtotal</span><span>{money(detail.totalAmount)}</span></div>
              <div className="flex justify-between text-content-secondary"><span>Desconto</span><span>- {money(detail.discountAmount)}</span></div>
              <div className="flex justify-between font-semibold text-content-primary"><span>Total</span><span>{money(detail.finalAmount)}</span></div>
            </div>

            {/* Parcelas */}
            {detail.installmentSchedule.length > 0 && (
              <div>
                <h4 className="mb-1.5 font-medium text-content-primary">Parcelas</h4>
                <div className="overflow-hidden rounded-card border border-border-subtle">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-bg-surface-raised text-content-secondary">
                      <tr><th className="px-2 py-1.5">#</th><th className="px-2 py-1.5">Vencimento</th><th className="px-2 py-1.5">Bruto</th><th className="px-2 py-1.5">Líquido</th><th className="px-2 py-1.5">Status</th></tr>
                    </thead>
                    <tbody className="divide-y divide-border-subtle">
                      {detail.installmentSchedule.map((p) => (
                        <tr key={p.id}>
                          <td className="px-2 py-1.5">{p.installmentNum}</td>
                          <td className="px-2 py-1.5">{dateOnly(p.dueDate)}</td>
                          <td className="px-2 py-1.5">{money(p.grossAmount)}</td>
                          <td className="px-2 py-1.5">{money(p.netAmount)}</td>
                          <td className="px-2 py-1.5">{INSTALLMENT_STATUS_LABELS[p.status]}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {detail.notes && (
              <div>
                <span className="text-content-muted">Observações</span>
                <p className="whitespace-pre-wrap text-content-secondary">{detail.notes}</p>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Modal de cancelamento */}
      <Modal
        open={cancelId !== null}
        title="Cancelar venda"
        onClose={() => setCancelId(null)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setCancelId(null)} disabled={cancelMutation.isPending}>Voltar</button>
            <button
              className="btn-primary bg-state-danger hover:bg-red-600"
              onClick={() => cancelMutation.mutate()}
              disabled={cancelReason.trim().length < 5 || cancelMutation.isPending}
            >
              {cancelMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Confirmar cancelamento"}
            </button>
          </>
        }
      >
        <p className="mb-3 text-sm text-content-secondary">
          O estoque dos itens será estornado e as parcelas pendentes canceladas. Esta ação não pode ser desfeita.
        </p>
        <label className="mb-1.5 block text-sm text-content-secondary">Motivo <span className="text-brand-purple">*</span></label>
        <textarea
          className="input-base min-h-20"
          placeholder="Ex: cliente desistiu da compra"
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          autoFocus
        />
        {cancelReason.length > 0 && cancelReason.trim().length < 5 && (
          <p className="mt-1 text-xs text-state-danger">Mínimo de 5 caracteres.</p>
        )}
      </Modal>
    </div>
  );
}
