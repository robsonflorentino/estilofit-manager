import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { PackagePlus, Trash2, Plus, Loader2 } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { inventoryService } from "../services/inventoryService";
import { supplierService } from "../services/supplierService";
import { productService } from "../services/productService";
import { getApiErrorMessage } from "../lib/api";
import type { SupplyLotItemInput } from "../types/inventory";
import type { Variant } from "../types/product";

interface ItemRow extends SupplyLotItemInput {
  sku: string;
  label: string;
}

export function SupplyEntryPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [supplierId, setSupplierId] = useState("");
  const [receivedAt, setReceivedAt] = useState(new Date().toISOString().slice(0, 10));
  const [freightCost, setFreightCost] = useState("0");
  const [notes, setNotes] = useState("");
  const [items, setItems] = useState<ItemRow[]>([]);

  // seleção de variação
  const [productSearch, setProductSearch] = useState("");
  const [selectedProductId, setSelectedProductId] = useState("");
  const [selectedVariantId, setSelectedVariantId] = useState("");
  const [qty, setQty] = useState("1");
  const [unitCost, setUnitCost] = useState("");

  const { data: suppliers } = useQuery({
    queryKey: ["suppliers", "active-all"],
    queryFn: () => supplierService.list(0, 100, { active: true }),
  });

  const { data: products } = useQuery({
    queryKey: ["products", "for-entry", productSearch],
    queryFn: () => productService.list(0, 20, { name: productSearch || undefined, active: true }),
  });

  const { data: productDetail } = useQuery({
    queryKey: ["product", selectedProductId],
    queryFn: () => productService.getById(selectedProductId),
    enabled: !!selectedProductId,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      inventoryService.createLot({
        supplierId,
        receivedAt,
        freightCost: Number(freightCost) || 0,
        notes: notes || undefined,
        items: items.map((i) => ({ variantId: i.variantId, quantity: i.quantity, unitCost: i.unitCost })),
      }),
    onSuccess: () => {
      toast.success("Entrada registrada. Estoque e custos atualizados.");
      queryClient.invalidateQueries({ queryKey: ["supply-lots"] });
      queryClient.invalidateQueries({ queryKey: ["stock"] });
      navigate("/stock");
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível registrar a entrada.")),
  });

  const addItem = () => {
    const variant = productDetail?.variants.find((v: Variant) => v.id === selectedVariantId);
    if (!variant || !unitCost) {
      toast.error("Selecione a variação e informe o custo.");
      return;
    }
    if (items.some((i) => i.variantId === variant.id)) {
      toast.error("Essa variação já está na lista.");
      return;
    }
    setItems((prev) => [
      ...prev,
      {
        variantId: variant.id,
        quantity: Number(qty) || 1,
        unitCost: Number(unitCost),
        sku: variant.sku,
        label: `${productDetail?.name} · ${variant.size}/${variant.color}`,
      },
    ]);
    setSelectedVariantId("");
    setQty("1");
    setUnitCost("");
  };

  const removeItem = (variantId: string) =>
    setItems((prev) => prev.filter((i) => i.variantId !== variantId));

  const itemsTotal = items.reduce((acc, i) => acc + i.quantity * i.unitCost, 0);
  const grandTotal = itemsTotal + (Number(freightCost) || 0);

  const canSubmit = supplierId && items.length > 0 && !createMutation.isPending;

  const money = (n: number) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

  return (
    <div>
      <PageHeader
        icon={PackagePlus}
        title="Entrada de Mercadoria"
        description="Registre um lote recebido. O sistema rateia o frete, atualiza custo médio, preço e estoque."
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Dados do lote */}
        <div className="card space-y-4 lg:col-span-1">
          <h3 className="font-semibold text-content-primary">Dados do lote</h3>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Fornecedor *</label>
            <select className="input-base" value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
              <option value="">Selecione...</option>
              {suppliers?.content.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Data de recebimento *</label>
            <input type="date" className="input-base" value={receivedAt} onChange={(e) => setReceivedAt(e.target.value)} />
          </div>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Frete total (R$)</label>
            <input type="number" step="0.01" min="0" className="input-base" value={freightCost} onChange={(e) => setFreightCost(e.target.value)} />
          </div>
          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Observações</label>
            <textarea className="input-base min-h-16" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Pedido #, condições..." />
          </div>
        </div>

        {/* Itens */}
        <div className="card space-y-4 lg:col-span-2">
          <h3 className="font-semibold text-content-primary">Itens recebidos</h3>

          {/* Adicionar item */}
          <div className="space-y-3 rounded-card border border-border-subtle bg-bg-input p-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Produto</label>
                <input className="input-base mb-2" placeholder="Buscar produto..." value={productSearch} onChange={(e) => setProductSearch(e.target.value)} />
                <select className="input-base" value={selectedProductId} onChange={(e) => { setSelectedProductId(e.target.value); setSelectedVariantId(""); }}>
                  <option value="">Selecione o produto...</option>
                  {products?.content.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Variação</label>
                <select className="input-base" value={selectedVariantId} onChange={(e) => setSelectedVariantId(e.target.value)} disabled={!selectedProductId}>
                  <option value="">Selecione a variação...</option>
                  {productDetail?.variants.filter((v) => v.active).map((v) => (
                    <option key={v.id} value={v.id}>{v.sku} · {v.size}/{v.color}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Quantidade</label>
                <input type="number" min="1" className="input-base" value={qty} onChange={(e) => setQty(e.target.value)} />
              </div>
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Custo unitário (R$)</label>
                <input type="number" step="0.01" min="0.01" className="input-base" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} />
              </div>
              <div className="flex items-end">
                <button className="btn-secondary w-full" onClick={addItem} type="button">
                  <Plus className="h-4 w-4" /> Adicionar
                </button>
              </div>
            </div>
          </div>

          {/* Lista de itens */}
          {items.length > 0 ? (
            <div className="overflow-hidden rounded-card border border-border">
              <table className="w-full text-left text-sm">
                <thead className="bg-bg-surface-raised text-xs uppercase tracking-wider text-content-secondary">
                  <tr>
                    <th className="px-3 py-2">Item</th>
                    <th className="px-3 py-2">Qtd</th>
                    <th className="px-3 py-2">Custo un.</th>
                    <th className="px-3 py-2">Subtotal</th>
                    <th className="px-3 py-2"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border-subtle">
                  {items.map((i) => (
                    <tr key={i.variantId} className="bg-bg-surface">
                      <td className="px-3 py-2">
                        <span className="font-mono text-xs text-brand-purple">{i.sku}</span>
                        <span className="ml-2 text-content-secondary">{i.label}</span>
                      </td>
                      <td className="px-3 py-2">{i.quantity}</td>
                      <td className="px-3 py-2">{money(i.unitCost)}</td>
                      <td className="px-3 py-2">{money(i.quantity * i.unitCost)}</td>
                      <td className="px-3 py-2 text-right">
                        <button onClick={() => removeItem(i.variantId)} className="rounded-btn p-1.5 text-content-secondary hover:text-state-danger" title="Remover">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-content-muted">Nenhum item adicionado ainda.</p>
          )}

          {/* Totais */}
          <div className="space-y-1 border-t border-border-subtle pt-3 text-sm">
            <div className="flex justify-between text-content-secondary"><span>Mercadoria</span><span>{money(itemsTotal)}</span></div>
            <div className="flex justify-between text-content-secondary"><span>Frete</span><span>{money(Number(freightCost) || 0)}</span></div>
            <div className="flex justify-between font-semibold text-content-primary"><span>Total</span><span>{money(grandTotal)}</span></div>
          </div>

          <div className="flex justify-end gap-3">
            <button className="btn-secondary" onClick={() => navigate("/stock")} disabled={createMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={() => createMutation.mutate()} disabled={!canSubmit}>
              {createMutation.isPending ? <><Loader2 className="h-4 w-4 animate-spin" /> Registrando...</> : "Registrar entrada"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
