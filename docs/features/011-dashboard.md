# Feature 011 — Dashboard (KPIs) (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-dashboard`                     |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `dashboard` + tela no frontend      |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Vendas (010), Estoque (009)                 |

---

## Objetivo

Ligar os 4 KPIs do painel (antes placeholders) a dados reais do mês corrente,
respeitando o papel do usuário: vendedores veem apenas os próprios números de venda
e não têm acesso aos indicadores de gestão (estoque e pró-labore).

---

## KPIs

| KPI | Cálculo | Visível para |
|-----|---------|--------------|
| Vendas do mês (R$) | `SUM(finalAmount)` de vendas `CONFIRMED` no mês | Todos (vendedor: só as próprias) |
| Nº de vendas | `COUNT` de vendas `CONFIRMED` no mês | Todos (vendedor: só as próprias) |
| Itens em estoque | `SUM(stockQuantity)` das variações ativas | Admin + Gestor |
| Pró-labore estimado | `lucro × PRO_LABORE_PCT`, lucro = faturamento − custo das mercadorias vendidas | Admin + Gestor |

- O mês corrente é a janela `[primeiro dia 00:00, primeiro dia do mês seguinte 00:00)` sobre `confirmedAt`.
- Custo das mercadorias vendidas = `SUM(quantidade × custo médio da variação)` dos itens das vendas confirmadas.
- Pró-labore usa piso zero (lucro negativo não gera valor negativo). Percentual configurável em `system_settings` (chave `PRO_LABORE_PCT`, default 30).

---

## Backend (`dashboard/`)

### Componentes
- `dto/DashboardKpisResponse` — `monthRevenue`, `saleCount`, `stockItems?`, `estimatedProLabore?` (os dois últimos nulos para vendedor)
- `service/DashboardService` — resolve o usuário logado, aplica a regra de papel e monta os KPIs
- `controller/DashboardController` — `GET /dashboard/kpis` (autenticado)

### Alterações em módulos existentes
- `SaleRepository.aggregateConfirmed(start, end, sellerId)` → projeção `SalesAggregate` (revenue, saleCount, cost). Exclui canceladas; filtro opcional por vendedor no padrão `:sellerId IS NULL OR ...`; custo via subquery com `COALESCE` (variações sem custo entram como 0)
- `ProductVariantRepository.sumActiveStock()` → `SUM(stockQuantity)` das variações ativas
- `SettingsReader.proLaborePct()` → lê `PRO_LABORE_PCT` (fallback 30)
- Migration `V13__insert_pro_labore_setting.sql` semeia a chave (`ON CONFLICT DO NOTHING`)

### Regra de papel
Igual à de Vendas: o service busca o usuário por e-mail; se `Role.SELLER`, força o filtro por `seller` e devolve `stockItems`/`estimatedProLabore` nulos.

### Endpoint
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/dashboard/kpis` | 🟢 Todos (vendedor recebe só os próprios indicadores de venda) |

---

## Frontend

- `types/dashboard.ts`, `services/dashboardService.ts`
- `pages/DashboardPage.tsx` — liga os 4 cards via React Query, com skeleton no carregamento. Os cards de estoque e pró-labore só aparecem quando o backend envia os valores (não nulos), ou seja, apenas para gestão. `money()` trata `null`/`undefined` com `== null`.

---

## Verificação Realizada

### Testes — 106 no total (6 novos), 0 falhas
- `DashboardIntegrationTest` (6): gestor recebe estoque e pró-labore não nulos; vendedor recebe ambos nulos; KPIs do vendedor refletem só as próprias vendas (vendedor novo começa zerado; 2 vendas → R$300); venda cancelada sai do faturamento; estoque global cresce ao menos o incremento próprio; sem token → 401

### Manual (API real) — cenário controlado validado
- Vendedor após 1 venda de 4un × R$100: `monthRevenue=400`, `saleCount=1`, `stockItems=null`, `estimatedProLabore=null`
- Admin (global): `monthRevenue=547,48`, `saleCount=2`, `stockItems=103`, `estimatedProLabore=82,12`
- Conferência do pró-labore: 82,12 = 30% de R$273,73 de lucro (R$200 da venda do vendedor + lucro da venda preexistente) — fórmula correta
- Telas compilam no Vite; typecheck 0 erros

---

## Notas Técnicas
- Os testes de KPI de gestão usam asserções `>=` porque o mês corrente é um estado compartilhado no banco de teste (sem paralelismo no JUnit); os testes de vendedor são determinísticos (vendedor novo, isolado). O valor exato do pró-labore foi validado no teste manual.
- `SettingsReader` segue a decisão 3 da feature 009 (leitura direta de `system_settings` enquanto não há módulo de Settings). Quando o módulo existir, `PRO_LABORE_PCT` passa a ser editável pela tela de configurações.

---

## Próximos Passos
Próximo módulo natural: **Relatórios** (faturamento por período com filtros, ticket médio,
produtos mais vendidos, vendas por canal e forma de pagamento) — a tela `/relatorios` já
está no menu aguardando implementação.
