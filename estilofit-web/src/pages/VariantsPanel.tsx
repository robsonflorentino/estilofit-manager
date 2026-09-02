import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Plus, Power, Loader2 } from "lucide-react";
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
                    <th className="px-3 py-2">Preço</th>
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
                      <td className="px-3 py-2">{fmt(v.salePrice)}</td>
                      <td className="px-3 py-2">{v.stockQuantity}</td>
                      <td className="px-3 py-2">
                        {v.active ? <Badge variant="success">Ativa</Badge> : <Badge variant="danger">Inativa</Badge>}
                      </td>
                      <td className="px-3 py-2 text-right">
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
    </Modal>
  );
}
