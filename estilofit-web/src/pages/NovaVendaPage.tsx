import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { ShoppingCart, Trash2, Plus, Loader2, AlertTriangle } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { saleService } from "../services/saleService";
import { productService } from "../services/productService";
import { getApiErrorMessage } from "../lib/api";
import type { PaymentMethod, FreightType } from "../types/sale";
import { PAYMENT_METHOD_LABELS, FREIGHT_TYPE_LABELS } from "../types/sale";
import type { Variant } from "../types/product";

interface CartRow {
  variantId: string;
  quantity: number;
  unitPrice: number; // snapshot do salePrice da variação
  stockQuantity: number;
  sku: string;
  label: string;
}

const money = (n: number | null | undefined) =>
  n == null ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "PIX", "DEBIT_CARD", "CREDIT_CARD", "TRANSFER"];

export function NovaVendaPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [channelId, setChannelId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("PIX");
  const [installments, setInstallments] = useState("1");
  const [cardFeePct, setCardFeePct] = useState("");
  const [cardFeePassed, setCardFeePassed] = useState(true);
  const [discount, setDiscount] = useState("0");
  const [freightType, setFreightType] = useState<FreightType>("NONE");
  const [freightAmount, setFreightAmount] = useState("");
  const [notes, setNotes] = useState("");
  const [cart, setCart] = useState<CartRow[]>([]);

  // seleção de variação
  const [productSearch, setProductSearch] = useState("");
  const [selectedProductId, setSelectedProductId] = useState("");
  const [selectedVariantId, setSelectedVariantId] = useState("");
  const [qty, setQty] = useState("1");

  const { data: channels } = useQuery({
    queryKey: ["sale-channels", "active"],
    queryFn: () => saleService.listChannels(false),
  });

  const { data: products } = useQuery({
    queryKey: ["products", "for-sale", productSearch],
    queryFn: () => productService.list(0, 20, { name: productSearch || undefined, active: true }),
  });

  const { data: productDetail } = useQuery({
    queryKey: ["product", selectedProductId],
    queryFn: () => productService.getById(selectedProductId),
    enabled: !!selectedProductId,
  });

  const isCreditCard = paymentMethod === "CREDIT_CARD";
  const nInstallments = Number(installments) || 1;
  const isInstallment = isCreditCard && nInstallments >= 2;

  const createMutation = useMutation({
    mutationFn: () =>
      saleService.create({
        channelId,
        paymentMethod,
        installments: isCreditCard ? nInstallments : 1,
        cardFeePct: isCreditCard && cardFeePct ? Number(cardFeePct) : null,
        cardFeePassed,
        discountAmount: Number(discount) || 0,
        freightType,
        freightAmount: freightType === "PAID" ? Number(freightAmount) || 0 : 0,
        notes: notes || undefined,
        items: cart.map((c) => ({ variantId: c.variantId, quantity: c.quantity })),
      }),
    onSuccess: (sale) => {
      toast.success("Venda registrada. Estoque baixado.");
      queryClient.invalidateQueries({ queryKey: ["sales"] });
      queryClient.invalidateQueries({ queryKey: ["stock"] });
      queryClient.invalidateQueries({ queryKey: ["installments"] });
      navigate(`/sales?highlight=${sale.id}`);
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível registrar a venda.")),
  });

  const addItem = () => {
    const variant = productDetail?.variants.find((v: Variant) => v.id === selectedVariantId);
    if (!variant) {
      toast.error("Selecione a variação.");
      return;
    }
    if (variant.salePrice == null) {
      toast.error(`A variação ${variant.sku} não tem preço de venda definido.`);
      return;
    }
    if (cart.some((c) => c.variantId === variant.id)) {
      toast.error("Essa variação já está no carrinho.");
      return;
    }
    const wanted = Number(qty) || 1;
    if (variant.stockQuantity < wanted) {
      toast.error(`Estoque insuficiente para ${variant.sku} (disponível: ${variant.stockQuantity}).`);
      return;
    }
    setCart((prev) => [
      ...prev,
      {
        variantId: variant.id,
        quantity: wanted,
        unitPrice: variant.salePrice!,
        stockQuantity: variant.stockQuantity,
        sku: variant.sku,
        label: `${productDetail?.name} · ${variant.size}/${variant.color}`,
      },
    ]);
    setSelectedVariantId("");
    setQty("1");
  };

  const removeItem = (variantId: string) =>
    setCart((prev) => prev.filter((c) => c.variantId !== variantId));

  const subtotal = cart.reduce((acc, c) => acc + c.quantity * c.unitPrice, 0);
  const discountValue = Number(discount) || 0;
  const finalAmount = Math.max(subtotal - discountValue, 0);
  const freightValue = freightType === "PAID" ? Number(freightAmount) || 0 : 0;
  const totalPaid = finalAmount + freightValue;

  // Prévia do parcelamento
  const installmentPreview = useMemo(() => {
    if (!isInstallment) return null;
    const per = finalAmount / nInstallments;
    const fee = Number(cardFeePct) || 0;
    const netPer = per * (1 - fee / 100);
    return { per, netPer, fee };
  }, [isInstallment, finalAmount, nInstallments, cardFeePct]);

  const canSubmit =
    channelId &&
    cart.length > 0 &&
    finalAmount > 0 &&
    (!isInstallment || (cardFeePct !== "" && Number(cardFeePct) >= 0)) &&
    (freightType !== "PAID" || freightValue > 0) &&
    !createMutation.isPending;

  return (
    <div>
      <PageHeader
        icon={ShoppingCart}
        title="Nova Venda"
        description="Monte o carrinho. O preço vem automaticamente do cadastro da variação e o estoque é baixado ao confirmar."
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Itens do carrinho */}
        <div className="card space-y-4 lg:col-span-2">
          <h3 className="font-semibold text-content-primary">Itens da venda</h3>

          <div className="space-y-3 rounded-card border border-border-subtle bg-bg-input p-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Produto</label>
                <input
                  className="input-base mb-2"
                  placeholder="Buscar produto..."
                  value={productSearch}
                  onChange={(e) => setProductSearch(e.target.value)}
                />
                <select
                  className="input-base"
                  value={selectedProductId}
                  onChange={(e) => {
                    setSelectedProductId(e.target.value);
                    setSelectedVariantId("");
                  }}
                >
                  <option value="">Selecione o produto...</option>
                  {products?.content.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Variação</label>
                <select
                  className="input-base"
                  value={selectedVariantId}
                  onChange={(e) => setSelectedVariantId(e.target.value)}
                  disabled={!selectedProductId}
                >
                  <option value="">Selecione a variação...</option>
                  {productDetail?.variants
                    .filter((v) => v.active)
                    .map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.sku} · {v.size}/{v.color} — {money(v.salePrice)} (estq {v.stockQuantity})
                      </option>
                    ))}
                </select>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="mb-1 block text-xs text-content-secondary">Quantidade</label>
                <input
                  type="number"
                  min="1"
                  className="input-base"
                  value={qty}
                  onChange={(e) => setQty(e.target.value)}
                />
              </div>
              <div className="col-span-2 flex items-end">
                <button className="btn-secondary w-full" onClick={addItem} type="button">
                  <Plus className="h-4 w-4" /> Adicionar ao carrinho
                </button>
              </div>
            </div>
          </div>

          {cart.length > 0 ? (
            <div className="overflow-hidden rounded-card border border-border">
              <table className="w-full text-left text-sm">
                <thead className="bg-bg-surface-raised text-xs uppercase tracking-wider text-content-secondary">
                  <tr>
                    <th className="px-3 py-2">Item</th>
                    <th className="px-3 py-2">Qtd</th>
                    <th className="px-3 py-2">Preço un.</th>
                    <th className="px-3 py-2">Subtotal</th>
                    <th className="px-3 py-2"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border-subtle">
                  {cart.map((c) => (
                    <tr key={c.variantId} className="bg-bg-surface">
                      <td className="px-3 py-2">
                        <span className="font-mono text-xs text-brand-purple">{c.sku}</span>
                        <span className="ml-2 text-content-secondary">{c.label}</span>
                      </td>
                      <td className="px-3 py-2">{c.quantity}</td>
                      <td className="px-3 py-2">{money(c.unitPrice)}</td>
                      <td className="px-3 py-2">{money(c.quantity * c.unitPrice)}</td>
                      <td className="px-3 py-2 text-right">
                        <button
                          onClick={() => removeItem(c.variantId)}
                          className="rounded-btn p-1.5 text-content-secondary hover:text-state-danger"
                          title="Remover"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-content-muted">Carrinho vazio.</p>
          )}
        </div>

        {/* Pagamento + resumo */}
        <div className="card space-y-4 lg:col-span-1">
          <h3 className="font-semibold text-content-primary">Pagamento</h3>

          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Canal *</label>
            <select className="input-base" value={channelId} onChange={(e) => setChannelId(e.target.value)}>
              <option value="">Selecione...</option>
              {channels?.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Forma de pagamento *</label>
            <select
              className="input-base"
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{PAYMENT_METHOD_LABELS[m]}</option>
              ))}
            </select>
          </div>

          {isCreditCard && (
            <>
              <div>
                <label className="mb-1.5 block text-sm text-content-secondary">Parcelas</label>
                <select className="input-base" value={installments} onChange={(e) => setInstallments(e.target.value)}>
                  {Array.from({ length: 12 }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>{n}x</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1.5 block text-sm text-content-secondary">
                  Taxa da maquininha (%) {isInstallment && "*"}
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="input-base"
                  value={cardFeePct}
                  onChange={(e) => setCardFeePct(e.target.value)}
                  placeholder="ex: 3.5"
                />
              </div>
              <label className="flex items-center gap-2 text-sm text-content-secondary">
                <input type="checkbox" checked={cardFeePassed} onChange={(e) => setCardFeePassed(e.target.checked)} />
                Taxa repassada ao cliente (informativo)
              </label>
            </>
          )}

          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Desconto (R$)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              className="input-base"
              value={discount}
              onChange={(e) => setDiscount(e.target.value)}
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Frete</label>
            <select
              className="input-base"
              value={freightType}
              onChange={(e) => setFreightType(e.target.value as FreightType)}
            >
              {(["NONE", "FREE", "PAID"] as FreightType[]).map((f) => (
                <option key={f} value={f}>{FREIGHT_TYPE_LABELS[f]}</option>
              ))}
            </select>
            {freightType === "PAID" && (
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="input-base mt-2"
                value={freightAmount}
                onChange={(e) => setFreightAmount(e.target.value)}
                placeholder="Valor do frete (R$) — ex: 25,00"
              />
            )}
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-content-secondary">Observações</label>
            <textarea
              className="input-base min-h-16"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Cliente, entrega..."
            />
          </div>

          {/* Resumo */}
          <div className="space-y-1 border-t border-border-subtle pt-3 text-sm">
            <div className="flex justify-between text-content-secondary">
              <span>Subtotal</span><span>{money(subtotal)}</span>
            </div>
            <div className="flex justify-between text-content-secondary">
              <span>Desconto</span><span>- {money(discountValue)}</span>
            </div>
            <div className="flex justify-between text-content-secondary">
              <span>Produtos</span><span>{money(finalAmount)}</span>
            </div>
            {freightType !== "NONE" && (
              <div className="flex justify-between text-content-secondary">
                <span>Frete{freightType === "FREE" ? " (grátis)" : ""}</span>
                <span>{freightType === "FREE" ? money(0) : money(freightValue)}</span>
              </div>
            )}
            <div className="flex justify-between font-semibold text-content-primary">
              <span>Total a pagar</span><span>{money(totalPaid)}</span>
            </div>
            {installmentPreview && (
              <div className="mt-2 rounded-card bg-bg-input p-2 text-xs text-content-secondary">
                {nInstallments}x de {money(installmentPreview.per)}
                {installmentPreview.fee > 0 && (
                  <> · líquido/parcela ~ {money(installmentPreview.netPer)}</>
                )}
              </div>
            )}
          </div>

          {isInstallment && cardFeePct === "" && (
            <p className="flex items-center gap-1.5 text-xs text-state-warning">
              <AlertTriangle className="h-3.5 w-3.5" /> Informe a taxa da maquininha para parcelar.
            </p>
          )}

          <div className="flex justify-end gap-3">
            <button className="btn-secondary" onClick={() => navigate("/sales")} disabled={createMutation.isPending}>
              Cancelar
            </button>
            <button className="btn-primary" onClick={() => createMutation.mutate()} disabled={!canSubmit}>
              {createMutation.isPending ? (
                <><Loader2 className="h-4 w-4 animate-spin" /> Registrando...</>
              ) : (
                "Confirmar venda"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
