# Tech Design — Feature 010: Vendas + Contas a Receber

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/api-sales`                    |
| Status        | ✅ Design aprovado (2026-09-02) — todas as recomendações aceitas |
| Depende de    | Produtos/Variações (007), Estoque (009), Canais de venda (seed) |
| Regras        | RN-016 a RN-028                        |

> Documento de **design prévio** — decisões a validar **antes** de codar.

---

## 1. Objetivo

Registrar vendas (o pilar comercial): múltiplos itens, baixa atômica de estoque, desconto, e — para cartão parcelado — geração automática das parcelas (contas a receber). Inclui cancelamento com estorno e a baixa de parcelas.

---

## 2. Escopo proposto

Proponho entregar **Vendas + Contas a Receber juntas**, porque as parcelas nascem da venda e não fazem sentido isoladas. Um único pacote coeso:

- `POST /sales` — registrar venda (todos os perfis)
- `GET /sales` — listar (Gestor/Admin veem tudo; Vendedor só as próprias)
- `GET /sales/{id}` — detalhe
- `PATCH /sales/{id}/cancel` — cancelar (Admin + Gestor)
- `GET /installments` — listar parcelas (Admin + Gestor)
- `PATCH /installments/{id}/receive` — dar baixa (Admin + Gestor)
- `GET /installments/projected` — fluxo de caixa projetado por mês (Admin + Gestor)

**Confirma esse escopo único, ou prefere separar Contas a Receber numa feature à parte?** (recomendo juntas)

---

## 3. Modelo (tabelas já existem — migrations V8 e V9)

```
sales                          sale_items
├── id                         ├── id
├── channel_id (FK)            ├── sale_id (FK)
├── seller_id (FK user)        ├── variant_id (FK)
├── confirmed_at               ├── quantity
├── total_amount               ├── unit_price   (snapshot no momento da venda)
├── discount_amount            └── total_price
├── final_amount
├── payment_method             sale_installments
├── installments               ├── id
├── card_fee_pct               ├── sale_id (FK)
├── card_fee_passed            ├── installment_num
├── status                     ├── due_date
├── notes                      ├── gross_amount
├── cancelled_by / cancelled_at├── net_amount
└── created_at                 ├── status (PENDING/RECEIVED/CANCELLED)
                               ├── received_at / received_by
```

Nenhuma migration nova.

---

## 4. Fluxo do Registro de Venda (POST /sales) — atômico

```
1. Valida canal (existe e ativo)
2. Para cada item: valida variação (existe, ativa) e ESTOQUE SUFICIENTE (RN-015/020)
   → se qualquer item não tem estoque, falha tudo (422), nada é gravado
3. Calcula:
   total_amount = Σ (unit_price × quantity)   [unit_price = preço vigente da variação — snapshot]
   final_amount = total_amount - discount_amount
4. Se CREDIT_CARD parcelado (installments >= 2):
   - card_fee_pct obrigatório
   - gera parcelas D+30, D+60... (RN-024)
   - valor parcela = final_amount / n ; net = valor × (1 - fee/100)
5. Debita estoque de cada variação + movimentação SALE (negativa)
6. Salva venda (CONFIRMED), itens e parcelas
Tudo em @Transactional.
```

---

## 5. Pontos que preciso que você decida

### 5.1. Preço unitário: snapshot automático ou informado?
A RN-018 diz que o preço é o **vigente da variação no momento da venda**.
- **Opção A:** o vendedor **não informa** preço; o sistema pega o `salePrice` atual da variação automaticamente. Mais seguro (evita erro de digitação), fiel à RN-018.
- **Opção B:** o vendedor informa o preço (permite negociar na hora).
- **Recomendo A** — pega o preço da variação. Se precisar de desconto, usa o campo de desconto da venda. (E se a variação não tem preço ainda? Ver 5.2.)

### 5.2. Vender variação sem preço definido
Uma variação sem entrada de mercadoria tem `salePrice = null`.
- **Recomendo bloquear** (422): "variação X não tem preço definido". Não faz sentido vender sem preço.
- Confirma?

### 5.3. Desconto: valor fixo, percentual, ou os dois?
A RN-019 menciona ambos.
- **Recomendo simplificar:** o request envia `discountAmount` (valor fixo em R$). Se o frontend quiser oferecer "10%", ele calcula e manda o valor. Backend valida que `final_amount > 0`.
- Aceita valor fixo, ou quer os dois no backend?

### 5.4. Estoque insuficiente — falha total ou parcial?
- **Recomendo falha total (422)**: se qualquer item não tem estoque, a venda inteira é rejeitada, nada é gravado. Simples e seguro (transação atômica).
- Confirma?

### 5.5. Cancelamento — janela de tempo?
A RN-021 permite Admin/Gestor cancelar.
- **Recomendo:** sem janela de tempo — pode cancelar a qualquer momento enquanto status = CONFIRMED. Restaura estoque, cancela parcelas pendentes (RN-027).
- Confirma?

### 5.6. Taxa da maquininha repassada
`card_fee_passed = true` (padrão): a taxa já está embutida no preço, cliente paga. O `net_amount` da parcela desconta a taxa (o que a loja realmente recebe da operadora).
- **Recomendo:** sempre calcular `net_amount` com a taxa (é o valor real que entra), independente de repassada ou não. O `card_fee_passed` fica só como informação/registro.
- Confirma?

### 5.7. Vendedor vê só as próprias vendas
RN (perfis): Vendedor lista apenas as vendas que ele registrou.
- **Recomendo:** no `GET /sales`, se o usuário é SELLER, filtra automaticamente por `seller_id = usuário logado` (ignora qualquer filtro de sellerId que ele tente passar).
- Confirma?

---

## 6. Estrutura de Código Proposta

```
sale/
├── domain/
│   ├── Sale.kt
│   ├── SaleItem.kt
│   ├── SaleInstallment.kt
│   ├── PaymentMethod.kt (enum)
│   ├── SaleStatus.kt (enum)
│   └── InstallmentStatus.kt (enum)
├── repository/
│   ├── SaleRepository.kt
│   └── SaleInstallmentRepository.kt
├── dto/
│   ├── SaleDTOs.kt
│   └── InstallmentDTOs.kt
├── service/
│   ├── InstallmentScheduler.kt   ← geração de parcelas isolada e testável
│   ├── SaleService.kt            ← registro/cancelamento (transacional)
│   └── InstallmentService.kt     ← listagem, baixa, fluxo projetado
└── controller/
    ├── SaleController.kt
    └── InstallmentController.kt
```

`InstallmentScheduler` isolado (como `FreightAllocator` e `SkuGenerator`) — concentra o cálculo de parcelas (vencimentos, bruto/líquido) para teste unitário.

A movimentação de estoque na venda reusa o `StockMovement` do módulo inventory (tipo SALE). Vou expor um método no `StockService`/inventory para debitar/creditar, evitando duplicar lógica.

---

## 7. Decisões a Confirmar (resumo)

1. Escopo único (Vendas + Contas a Receber juntas)? (recomendo sim)
2. Preço = snapshot automático da variação (não informado pelo vendedor)? (recomendo sim)
3. Bloquear venda de variação sem preço (422)? (recomendo sim)
4. Desconto como valor fixo em R$ (frontend calcula % se quiser)? (recomendo sim)
5. Estoque insuficiente = falha total da venda (422)? (recomendo sim)
6. Cancelamento sem janela de tempo (enquanto CONFIRMED)? (recomendo sim)
7. `net_amount` sempre desconta a taxa; `card_fee_passed` é informativo? (recomendo sim)
8. Vendedor lista só as próprias vendas (filtro forçado)? (recomendo sim)

Responda (ou "segue as recomendações") e eu parto para a implementação com testes.
