# Feature 012 — Relatórios (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-reports`                       |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `report` + tela no frontend         |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Vendas (010), Estoque (009)                 |

---

## Objetivo

Tela `/reports` (Admin + Gestor) com relatórios de vendas sobre um período selecionável
(padrão: mês corrente), com gráficos. Consome os dados de vendas confirmadas.

---

## Relatórios (todos por período `startDate`/`endDate`, apenas vendas confirmadas)

1. **Resumo** — faturamento, nº de vendas, ticket médio (faturamento / nº vendas) e lucro estimado (faturamento − custo das mercadorias vendidas)
2. **Faturamento por dia** — série temporal (gráfico de linha)
3. **Produtos mais vendidos** — ranking por quantidade (gráfico de barras horizontal), top 10
4. **Vendas por canal** — participação no faturamento (gráfico de pizza)
5. **Vendas por forma de pagamento** — participação no faturamento (gráfico de pizza)

O período é `[startDate 00:00, endDate+1 00:00)` — fim inclusivo no dia.

---

## Fix incluído — `TRANSFER`
A feature 010 listava `TRANSFER` no frontend, mas o enum `PaymentMethod` do backend só tinha
4 valores e havia um CHECK constraint no banco. Corrigido:
- `PaymentMethod` ganhou `TRANSFER`
- Migration `V14__add_transfer_payment_method.sql` recria o CHECK `ck_sales_payment` incluindo `TRANSFER`

---

## Backend (`report/`)

### Componentes
- `dto/ReportDTOs` — `ReportSummaryResponse`, `DailyRevenueResponse`, `TopProductResponse`, `RevenueSliceResponse` (com `percentage`)
- `service/ReportService` — converte o intervalo em janela e monta cada relatório; `toSlices` calcula a participação percentual sobre o total
- `controller/ReportController` — 5 endpoints GET (Admin + Gestor)

### Queries de agregação
- `SaleRepository.revenueByDay` — `GROUP BY CAST(confirmedAt AS date)`
- `SaleRepository.revenueByChannel` — `GROUP BY channel.name`
- `SaleRepository.revenueByPayment` — `GROUP BY paymentMethod`
- `SaleItemRepository.topProducts` — `GROUP BY` variação, ordenado por quantidade (Pageable para o limite)
- Resumo reaproveita `SaleRepository.aggregateConfirmed` (criado na feature 011)
- Todas excluem canceladas (`status = CONFIRMED`) e filtram por `confirmedAt` no período

### Endpoints
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/reports/summary` | 🟡 Admin + Gestor |
| GET | `/reports/revenue-by-day` | 🟡 Admin + Gestor |
| GET | `/reports/top-products?limit=10` | 🟡 Admin + Gestor |
| GET | `/reports/by-channel` | 🟡 Admin + Gestor |
| GET | `/reports/by-payment` | 🟡 Admin + Gestor |

`startDate` e `endDate` são obrigatórios (ISO date).

---

## Frontend

- Dependência nova: **Recharts** (`^3.10.1`)
- `types/report.ts`, `services/reportService.ts`
- `pages/ReportsPage.tsx` — seletor de período (padrão mês corrente), KPIs do resumo, gráfico de linha (faturamento/dia), barras horizontais (top produtos), duas pizzas (canal e pagamento). `money()` trata `null`/`undefined` com `== null`; formatters do Recharts normalizam valores possivelmente indefinidos
- Rota `/reports` (RoleRoute Admin + Gestor); o item de menu já existia

---

## Verificação Realizada

### Testes — 114 no total (8 novos), 0 falhas
- `ReportIntegrationTest` (8): resumo com deltas (faturamento +500, lucro +250, +2 vendas, ticket > 0); venda cancelada não entra; top-products ranqueia a variação (7 un, R$700); by-channel em canal exclusivo (R$400, 1 venda, % > 0); by-payment inclui `TRANSFER`; revenue-by-day inclui o dia de hoje; vendedor → 403; sem token → 401
- As queries com `CAST`/`GROUP BY` foram validadas contra o PostgreSQL real (Testcontainers)

### Manual (API real) — período do mês
- Resumo: faturamento R$1.027,48, 5 vendas, ticket médio R$205,50, lucro R$513,74
- Por canal (canal exclusivo do teste): R$480,00, 3 vendas, 46,72%
- Por pagamento: CASH R$640, PIX R$160, CREDIT_CARD R$147,48, **TRANSFER R$80** — fix validado de ponta a ponta; percentuais somam 100%
- Top produtos: variação vendida com 6 un e R$480,00
- Faturamento por dia: dia de hoje com R$1.027,48
- Frontend: typecheck 0 erros; build de produção do Vite OK

---

## Notas Técnicas
- Recharts 3.x tem tipagem mais estrita: os `formatter` de Tooltip recebem valores possivelmente indefinidos (normalizados com `Number()`), e os rótulos de Pie usam `name`/`percent` nativos em vez de campos customizados.
- O build gera um chunk > 500 kB (Recharts). É apenas um aviso; code-splitting pode ser aplicado depois se necessário.

---

## Próximos Passos
Sugestão: **Alertas/Promoções** (a rota `/promotions` já está no menu) — produtos sem venda
há X dias (RN de alerta de promoção), reaproveitando os dados de vendas e estoque.
