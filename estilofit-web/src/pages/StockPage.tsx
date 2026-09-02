import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { Package, PackagePlus, SlidersHorizontal, Loader2 } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { RoleGuard } from "../components/RoleGuard";
import { inventoryService } from "../services/inventoryService";
import { categoryService } from "../services/categoryService";
import { getApiErrorMessage } from "../lib/api";
import type { StockSummaryItem } from "../types/inventory";

const PAGE_SIZE = 15;

const adjustSchema = z.object({
  quantity: z
    .string()
    .min(1, "Quantidade é obrigatória")
    .refine((v) => Number.isInteger(Number(v)) && Number(v) !== 0, "Quantidade deve ser um inteiro diferente de zero"),
  notes: z.string().min(5, "Justificativa é obrigatória (mínimo 5 caracteres)"),
});
type AdjustForm = z.infer<typeof adjustSchema>;

const money = (n: number | null) =>
  n === null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function StockPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [categoryId, setCategoryId] = useState("");
  const [lowStock, setLowStock] = useState(false);
  const [adjusting, setAdjusting] = useState<StockSummaryItem | null>(null);

  const filters = { categoryId: categoryId || undefined, lowStock };

  const { data, isLoading } = useQuery({
    queryKey: ["stock", page, filters],
    queryFn: () => inventoryService.stockSummary(page, PAGE_SIZE, filters),
  });

  const { data: categories = [] } = useQuery({
    queryKey: ["categories", { showInactive: false }],
    queryFn: () => categoryService.list(true),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AdjustForm>({ resolver: zodResolver(adjustSchema) });

  const adjustMutation = useMutation({
    mutationFn: (form: AdjustForm) =>
      inventoryService.adjust({ variantId: adjusting!.variantId, quantity: Number(form.quantity), notes: form.notes }),
    onSuccess: () => {
      toast.success("Estoque ajustado.");
      setAdjusting(null);
      reset();
      queryClient.invalidateQueries({ queryKey: ["stock"] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível ajustar o estoque.")),
  });

  const openAdjust = (item: StockSummaryItem) => {
    reset({ quantity: "", notes: "" });
    setAdjusting(item);
  };

  const columns: Column<StockSummaryItem>[] = [
    { header: "SKU", render: (i) => <span className="font-mono text-xs text-brand-purple">{i.sku}</span> },
    { header: "Produto", render: (i) => <span className="font-medium">{i.productName}</span> },
    { header: "Tam/Cor", render: (i) => <span className="text-content-secondary">{i.size} / {i.color}</span> },
    { header: "Custo médio", render: (i) => <span className="text-content-secondary">{money(i.averageCost)}</span> },
    { header: "Preço", render: (i) => money(i.salePrice) },
    {
      header: "Estoque",
      render: (i) =>
        i.isZeroStock ? (
          <Badge variant="danger">0</Badge>
        ) : i.isLowStock ? (
          <Badge variant="warning">{i.stockQuantity}</Badge>
        ) : (
          <span>{i.stockQuantity}</span>
        ),
    },
    {
      header: "Ações",
      className: "text-right",
      render: (i) => (
        <RoleGuard roles={["ADMIN", "MANAGER"]}>
          <button onClick={() => openAdjust(i)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple" title="Ajustar estoque">
            <SlidersHorizontal className="h-4 w-4" />
          </button>
        </RoleGuard>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Package}
        title="Estoque"
        description="Consulte o estoque atual por variação. Alertas de estoque baixo em destaque."
        action={
          <RoleGuard roles={["ADMIN", "MANAGER"]}>
            <button className="btn-primary" onClick={() => navigate("/stock/entry")}>
              <PackagePlus className="h-4 w-4" />
              Entrada de mercadoria
            </button>
          </RoleGuard>
        }
      />

      {/* Filtros */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className="input-base w-52" value={categoryId} onChange={(e) => { setCategoryId(e.target.value); setPage(0); }}>
          <option value="">Todas as categorias</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-content-secondary">
          <input type="checkbox" checked={lowStock} onChange={(e) => { setLowStock(e.target.checked); setPage(0); }} className="accent-brand-purple" />
          Apenas estoque baixo
        </label>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(i) => i.variantId}
        loading={isLoading}
        emptyMessage="Nenhuma variação em estoque."
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

      {/* Modal de ajuste manual */}
      <Modal
        open={adjusting !== null}
        title={`Ajustar estoque · ${adjusting?.sku ?? ""}`}
        onClose={() => setAdjusting(null)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setAdjusting(null)} disabled={adjustMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={handleSubmit((f) => adjustMutation.mutate(f))} disabled={adjustMutation.isPending}>
              {adjustMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Ajustar"}
            </button>
          </>
        }
      >
        <div className="mb-3 text-sm text-content-secondary">
          Estoque atual: <span className="text-content-primary">{adjusting?.stockQuantity}</span>
        </div>
        <form className="space-y-4" onSubmit={handleSubmit((f) => adjustMutation.mutate(f))} noValidate>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">
              Quantidade <span className="text-brand-purple">*</span>
              <span className="ml-1 text-xs text-content-muted">(positivo entra, negativo sai)</span>
            </label>
            <input type="number" className="input-base" autoFocus placeholder="Ex: -2 ou 5" {...register("quantity")} />
            {errors.quantity && <p className="mt-1 text-xs text-state-danger">{errors.quantity.message}</p>}
          </div>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Justificativa <span className="text-brand-purple">*</span></label>
            <textarea className="input-base min-h-16" placeholder="Ex: peças danificadas no inventário" {...register("notes")} />
            {errors.notes && <p className="mt-1 text-xs text-state-danger">{errors.notes.message}</p>}
          </div>
        </form>
      </Modal>
    </div>
  );
}
