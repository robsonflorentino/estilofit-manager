# Feature 010 — Vendas + Contas a Receber (Backend + Frontend)

| Campo         | Valor                                              |
|---------------|----------------------------------------------------|
| Branch        | `feature/api-sales`                                |
| Data          | 2026-09-02                                         |
| Módulos       | Backend `sale` + telas no frontend                 |
| Status        | ✅ Concluída e verificada                          |
| Depende de    | Estoque (009), Produtos/Variações (007)            |
| Design        | `docs/features/010-sales-design.md`                |
| Regras        | RN-016 a RN-028                                     |

---

## Objetivo

Registrar vendas de forma atômica (baixa de estoque + snapshot de preço + geração de
parcelas), controlar o cancelamento com estorno de estoque e oferecer o módulo de
contas a receber (parcelas do cartão de crédito) com baixa e fluxo de caixa projetado.

---

## Backend (`sale/`)

### Componentes
- `domain/`: enums `PaymentMethod`, `SaleStatus`, `InstallmentStatus`; entidades `SaleChannel`, `Sale`, `SaleItem`, `SaleInstallment`
- `repository/`: `SaleChannelRepository`, `SaleRepository` (filtros + `findById` com EntityGraph), `SaleInstallmentRepository` (filtros + `findPendingBetween`)
- `service/InstallmentScheduler` — geração do cronograma de parcelas, isolado e testável
- `service/SaleService` — registro atômico e cancelamento
- `service/InstallmentService` — listagem, baixa e fluxo projetado
- `service/SaleChannelService` — CRUD leve de canais
- `controller/SaleController`, `controller/InstallmentController`, `controller/SaleChannelController`

### Fluxo do registro de venda (atômico)
1. Valida canal ativo e vendedor (usuário logado)
2. Para cada item: carrega a variação, **snapshot do `salePrice`** (não vem do cliente — decisão 2), bloqueia variação inativa/sem preço (422 — decisão 3) e valida estoque (falha total se insuficiente — decisão 5)
3. Calcula `totalAmount`, aplica `discountAmount` (valor fixo R$ — decisão 4) → `finalAmount`
4. Valida parcelamento: só `CREDIT_CARD` e taxa da maquininha obrigatória (RN-022)
5. Cria os itens e baixa o estoque via `StockService.registerSaleExit` (movimentação `SALE`)
6. Gera as parcelas via `InstallmentScheduler` (D+30/60/90; líquido desconta a taxa)

### Fluxo do cancelamento (atômico)
1. Bloqueia se já cancelada
2. Estorna o estoque de cada item via `StockService.registerSaleReversal` (movimentação `ADJUSTMENT`, refType `SALE_CANCEL` — RN-021)
3. Cancela apenas as parcelas `PENDING`; as `RECEIVED` permanecem no histórico (RN-027)
4. Marca `CANCELLED`, registra quem/quando e anexa o motivo às observações

### Cálculo das parcelas (`InstallmentScheduler`)
- Vencimentos D+30, D+60, ... a partir da data da venda
- Bruto = `finalAmount / n`; a **última parcela absorve o resíduo** para somar exatamente o total
- Líquido = bruto × (1 − taxa/100) — a taxa da maquininha sempre desconta do líquido (decisão 7)

### Endpoints
| Método | Rota | Permissão |
|--------|------|-----------|
| POST | `/sales` | 🟢 Todos |
| GET | `/sales` | 🟢 Todos (vendedor vê só as próprias — decisão 8) |
| GET | `/sales/{id}` | 🟢 Todos (vendedor só as próprias → 404) |
| PATCH | `/sales/{id}/cancel` | 🟡 Admin + Gestor |
| GET | `/installments` | 🟡 Admin + Gestor |
| GET | `/installments/projected` | 🟡 Admin + Gestor |
| PATCH | `/installments/{id}/receive` | 🟡 Admin + Gestor |
| GET | `/sale-channels` | 🟢 Todos |
| POST | `/sale-channels` | 🟡 Admin + Gestor |
| PATCH | `/sale-channels/{id}/status` | 🟡 Admin + Gestor |

---

## Frontend

- `types/sale.ts`, `services/saleService.ts`
- `pages/NovaVendaPage.tsx` — carrinho: busca produto → variação (preço automático do cadastro, decisão 2), validação de preço/estoque na adição, desconto, forma de pagamento com parcelas + taxa (só cartão de crédito) e prévia do parcelamento
- `pages/SalesPage.tsx` — listagem com filtros (canal, pagamento, status), modal de detalhe (itens + parcelas) e cancelamento com motivo (Admin + Gestor)
- `pages/ReceivablesPage.tsx` — abas **Parcelas** (filtro por status, baixa via confirmação) e **Fluxo projetado** (3/6/12 meses, agrupado por mês com totais bruto/líquido)
- Rotas: `/sales` e `/sales/new` (todos os perfis), `/receivables` (Admin + Gestor)

---

## Verificação Realizada

### Testes — 100 no total (13 novos), 0 falhas
- `InstallmentSchedulerTest` (5): vencimentos D+30/60/90, soma dos brutos = `finalAmount`, líquido desconta a taxa, taxa zero, erro com menos de 2 parcelas
- `SaleIntegrationTest` (8, Testcontainers): venda à vista baixa estoque e não gera parcelas (+ snapshot de preço do backend); venda parcelada gera parcelas com bruto/líquido; variação sem preço → 422; estoque insuficiente → 422 sem debitar nada; cancelamento estorna estoque e cancela parcelas pendentes; vendedor só enxerga as próprias (lista + detalhe 404); baixa de parcela → `RECEIVED` e segunda baixa → 422; vendedor não pode cancelar → 403

### Manual (API real) — fluxo completo validado
- Venda cartão de crédito 3x, taxa 10%, 2un × R$147,48 → 201, total **R$294,96**
- Estoque baixou **8 → 6**
- 3 parcelas: venc. 02/10, 01/11, 01/12; bruto **98,32** cada (soma = 294,96); líquido **88,49** (−10%)
- Contas a receber: lista de pendentes e fluxo projetado agrupado por mês
- Baixa de parcela → `RECEIVED` com data de hoje
- Cancelamento → venda `CANCELLED`, parcela já recebida preservada, parcelas pendentes `CANCELLED`, estoque estornado **6 → 8**
- Telas compilam no Vite; typecheck 0 erros

---

## Notas Técnicas
- Os métodos `registerSaleExit`/`registerSaleReversal` no `StockService` usam `Propagation.MANDATORY`: só rodam dentro da transação da venda/cancelamento, garantindo atomicidade (estoque e venda gravam juntos ou nada grava).
- Lançamentos de venda e parcelas são imutáveis; correções via cancelamento (decisão 6).
- Jackson com `default-property-inclusion: always` mantém o contrato fiel aos tipos do frontend (campos nulos presentes). `money()` no front trata `null`/`undefined` com `== null`.
- Seed de canais de venda em `V12__insert_initial_data.sql` (Instagram, WhatsApp, Presencial).

---

## Próximos Passos
Com Vendas e Contas a Receber concluídos, o próximo módulo natural é **Relatórios/Dashboard**
(faturamento, ticket médio, produtos mais vendidos, alertas de promoção), consumindo os
dados de vendas e estoque já disponíveis.
