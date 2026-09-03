import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Plus, Power, Loader2, Tag, RotateCcw, AlertTriangle } from "lucide-react";
import { Modal } from "../components/Modal";
import { Badge } from "../components/Badge";
import { productService } from "../services/productService";
import { getApiErrorMessage } from "../lib/api";
import type { ProductSummary, Variant } from "../types/product";

const variantSchema = z.object({
  size: z.string().min(1, "Tamanho é obrigatório").max(10),
  color: z.string().min(1, "Cor é obrigatória").max(50),
  profitMargin: z.string().optional(),
});
type VariantForm = z.infer<typeof variantSchema>;

interface VariantsPanelProps {
  product: ProductSummary;
  onClose: () => void;
}

export function VariantsPanel({ product, onClose }: VariantsPanelProps) {
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);

  const { data: detail, isLoading } = useQuery({
    queryKey: ["product", product.id],
    queryFn: () => productService.getById(product.id),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["product", product.id] });
  };

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<VariantForm>({ resolver: zodResolver(variantSchema) });

  const addMutation = useMutation({
    mutationFn: (f: VariantForm) => {
      const margin = f.profitMargin && f.profitMargin.trim() !== "" ? Number(f.profitMargin) : null;
      return productService.createVariant(product.id, {
        size: f.size,
        color: f.color,
        profitMargin: margin,
      });
    },
    onSuccess: (v) => {
      toast.success(`Variação criada: ${v.sku}`);
      reset({ size: "", color: "", profitMargin: "" });
      setAdding(false);
      invalidate();
    },
    onError: (error) => {
      const message = getApiErrorMessage(error, "Não foi possível criar a variação.");
      if (message.toLowerCase().includes("já existe")) {
        setError("size", { message });
      } else {
        toast.error(message);
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: (v: Variant) => productService.updateVariantStatus(product.id, v.id, !v.active),
    onSuccess: () => invalidate(),
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  // Edição de preço de venda (manual / voltar ao sugerido)
  const [editingPrice, setEditingPrice] = useState<Variant | null>(null);
  const [priceInput, setPriceInput] = useState("");

  const priceMutation = useMutation({
    mutationFn: (args: { variant: Variant; salePrice?: number; resetToSuggested?: boolean }) =>
      productService.updateVariant(product.id, args.variant.id, {
        salePrice: args.salePrice,
        resetToSuggested: args.resetToSuggested,
      }),
    onSuccess: () => {
      toast.success("Preço atualizado.");
      setEditingPrice(null);
      invalidate();
    },
    onError: (error) => toast.error(getApiErrorMessage(error, "Não foi possível atualizar o preço.")),
  });

  const openPriceEditor = (v: Variant) => {
    setEditingPrice(v);
    setPriceInput(v.salePrice != null ? String(v.salePrice) : "");
  };

  const fmt = (n: number | null | undefined) =>
    n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

  return (
    <Modal open title={`Variações · ${product.name}`} onClose={onClose}>
      {isLoading ? (
        <div className="py-8 text-center">
          <Loader2 className="mx-auto h-6 w-6 animate-spin text-brand-purple" />
        </div>
      ) : (
        <div className="space-y-4">
          {/* Lista de variações */}
          {detail && detail.variants.length > 0 ? (
            <div className="overflow-hidden rounded-card border border-border">
              <table className="w-full text-left text-sm">
                <thead className="bg-bg-surface-raised text-xs uppercase tracking-wider text-content-secondary">
                  <tr>
                    <th className="px-3 py-2">SKU</th>
                    <th className="px-3 py-2">Tam.</th>
                    <th className="px-3 py-2">Cor</th>
                    <th className="px-3 py-2">Custo</th>
                    <th className="px-3 py-2">Preço de venda</th>
                    <th className="px-3 py-2">Estoque</th>
                    <th className="px-3 py-2">Status</th>
                    <th className="px-3 py-2 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border-subtle">
                  {detail.variants.map((v) => (
                    <tr key={v.id} className="bg-bg-surface">
                      <td className="px-3 py-2 font-mono text-xs text-brand-purple">{v.sku}</td>
                      <td className="px-3 py-2">{v.size}</td>
                      <td className="px-3 py-2">{v.color}</td>
                      <td className="px-3 py-2">{fmt(v.averageCost)}</td>
                      <td className="px-3 py-2">
                        <div className="flex items-center gap-1.5">
                          <span className="font-medium">{fmt(v.salePrice)}</span>
                          {v.priceOverride && (
                            <span title="Preço definido manualmente">
                              <Badge variant="warning">manual</Badge>
                            </span>
                          )}
                          {v.averageCost != null && v.salePrice != null && v.salePrice < v.averageCost && (
                            <span title="Preço abaixo do custo médio">
                              <AlertTriangle className="h-3.5 w-3.5 text-state-danger" />
                            </span>
                          )}
                        </div>
                        {v.priceOverride && v.suggestedPrice != null && (
                          <span className="text-[11px] text-content-muted">Sugerido: {fmt(v.suggestedPrice)}</span>
                        )}
                      </td>
                      <td className="px-3 py-2">{v.stockQuantity}</td>
                      <td className="px-3 py-2">
                        {v.active ? <Badge variant="success">Ativa</Badge> : <Badge variant="danger">Inativa</Badge>}
                      </td>
                      <td className="px-3 py-2 text-right">
                        <button
                          onClick={() => openPriceEditor(v)}
                          className="rounded-btn p-1.5 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple"
                          title="Editar preço de venda"
                        >
                          <Tag className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => statusMutation.mutate(v)}
                          className="rounded-btn p-1.5 text-content-secondary hover:bg-bg-surface-raised hover:text-content-primary"
                          title={v.active ? "Desativar" : "Ativar"}
                        >
                          <Power className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-content-muted">
              Nenhuma variação ainda. Adicione a primeira abaixo.
            </p>
          )}

          {/* Adicionar variação */}
          {adding ? (
            <form
              className="space-y-3 rounded-card border border-border-subtle bg-bg-input p-4"
              onSubmit={handleSubmit((f) => addMutation.mutate(f))}
              noValidate
            >
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="mb-1 block text-xs text-content-secondary">Tamanho *</label>
                  <input className="input-base" placeholder="M" {...register("size")} />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-content-secondary">Cor *</label>
                  <input className="input-base" placeholder="Azul" {...register("color")} />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-content-secondary">Margem % (opc.)</label>
                  <input className="input-base" type="number" step="0.01" placeholder="herda global" {...register("profitMargin")} />
                </div>
              </div>
              {errors.size && <p className="text-xs text-state-danger">{errors.size.message}</p>}
              {errors.color && <p className="text-xs text-state-danger">{errors.color.message}</p>}
              <p className="text-xs text-content-muted">O SKU é gerado automaticamente pelo sistema.</p>
              <div className="flex justify-end gap-2">
                <button type="button" className="btn-secondary" onClick={() => setAdding(false)} disabled={addMutation.isPending}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={addMutation.isPending}>
                  {addMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Adicionar"}
                </button>
              </div>
            </form>
          ) : (
            <button className="btn-secondary w-full" onClick={() => setAdding(true)}>
              <Plus className="h-4 w-4" />
              Adicionar variação
            </button>
          )}
        </div>
      )}

      {/* Editor de preço de venda (manual / voltar ao sugerido) */}
      {editingPrice && (
        <Modal
          open
          title={`Preço · ${editingPrice.sku}`}
          onClose={() => setEditingPrice(null)}
        >
          {(() => {
            const v = editingPrice;
            const priceNum = priceInput.trim() !== "" ? Number(priceInput) : null;
            const belowCost = priceNum != null && v.averageCost != null && priceNum < v.averageCost;
            const invalid = priceNum == null || Number.isNaN(priceNum) || priceNum <= 0;
            return (
              <div className="space-y-4">
                <div className="rounded-card border border-border-subtle bg-bg-input p-3 text-sm">
                  <div className="flex justify-between">
                    <span className="text-content-secondary">Custo médio</span>
                    <span className="font-medium">{fmt(v.averageCost)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-content-secondary">Preço sugerido (pela margem)</span>
                    <span className="font-medium">{fmt(v.suggestedPrice ?? v.salePrice)}</span>
                  </div>
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-content-secondary">
                    Preço de venda <span className="text-brand-purple">*</span>
                  </label>
                  <input
                    className="input-base"
                    type="number"
                    step="0.01"
                    min="0.01"
                    autoFocus
                    value={priceInput}
                    onChange={(e) => setPriceInput(e.target.value)}
                    placeholder="Ex: 150,00"
                  />
                  <p className="mt-1 text-xs text-content-muted">
                    Você pode praticar um preço acima ou abaixo do sugerido. Ao salvar, ele fica
                    fixo e não muda nas próximas entradas de mercadoria.
                  </p>
                  {belowCost && (
                    <p className="mt-1.5 flex items-center gap-1.5 text-xs text-state-danger">
                      <AlertTriangle className="h-3.5 w-3.5" />
                      Atenção: esse preço está abaixo do custo médio ({fmt(v.averageCost)}).
                    </p>
                  )}
                </div>

                <div className="flex flex-wrap items-center justify-between gap-2">
                  {v.priceOverride ? (
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={priceMutation.isPending}
                      onClick={() => priceMutation.mutate({ variant: v, resetToSuggested: true })}
                      title="Volta a seguir a margem; o preço passa a ser recalculado nas entradas"
                    >
                      <RotateCcw className="h-4 w-4" />
                      Voltar ao sugerido
                    </button>
                  ) : (
                    <span />
                  )}
                  <div className="flex gap-2">
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => setEditingPrice(null)}
                      disabled={priceMutation.isPending}
                    >
                      Cancelar
                    </button>
                    <button
                      type="button"
                      className="btn-primary"
                      disabled={priceMutation.isPending || invalid}
                      onClick={() => priceMutation.mutate({ variant: v, salePrice: priceNum! })}
                    >
                      {priceMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Salvar preço"}
                    </button>
                  </div>
                </div>
              </div>
            );
          })()}
        </Modal>
      )}
    </Modal>
  );
}
