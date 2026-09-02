# Feature 016 — Lucratividade por Canal (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-profit-by-channel`             |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `report` + telas (Dashboard e Relatórios) |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Relatórios (012), Vendas (010)              |

---

## Objetivo

Mostrar qual canal de venda é o **mais lucrativo** (não só o que mais fatura), calculando o
lucro = faturamento − custo das mercadorias vendidas por canal. Aparece de forma compacta no
Dashboard (mês corrente) e detalhada nos Relatórios (período selecionável).

---

## Backend

### Query e cálculo
- `SaleRepository.profitByChannel(start, end)` → por canal: faturamento, custo (subquery de custo médio × quantidade) e nº de vendas; apenas vendas confirmadas
- `ReportService.profitByChannel(startDate, endDate)` → `ChannelProfitResponse` (canal, faturamento, custo, lucro, margem %, nº de vendas), ordenado por **lucro decrescente**
- Lucro = faturamento − custo; margem % = lucro / faturamento × 100

### Endpoint
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/reports/profit-by-channel?startDate&endDate` | 🟡 Admin + Gestor |

---

## Frontend

- Tipos em `types/report.ts`, `reportService.profitByChannel(period)`
- **Relatórios** (`ReportsPage`): seção "Lucratividade por canal" com gráfico de barras horizontais (lucro) + tabela detalhada (canal, faturamento, custo, lucro, margem %, vendas)
- **Dashboard** (`DashboardPage`): card compacto "Lucro por canal (mês)" — barras horizontais do mês corrente, destacando o canal campeão; visível apenas para Admin/Gestor
- `money()` trata `null`/`undefined` com `== null`

---

## Verificação Realizada

### Testes — 137 no total (4 novos), 0 falhas
- `ReportProfitByChannelTest` (4): lucro = receita − custo e margem % corretos (custo 40 / preço 80, 5 un → receita 400, custo 200, lucro 200, margem 50%); resultado ordenado por lucro desc; venda cancelada não entra; vendedor → 403

### Manual (API real)
- Dois canais no mês: PcLoja (8 un → lucro R$320) e PcInsta (3 un → lucro R$120); a lista veio ordenada por lucro desc (PcLoja antes de PcInsta), margens 50%
- Endpoint do Dashboard (mês corrente) retorna o canal mais lucrativo no topo
- Frontend: typecheck 0 erros

---

## Notas Técnicas
- "Mais lucrativo" ordena por **lucro absoluto (R$)**; a **margem %** também é exibida como coluna, pois um canal pode faturar muito com margem baixa.
- O card do Dashboard e a seção dos Relatórios consomem o mesmo endpoint, mudando apenas o período (mês corrente vs. selecionável) e a densidade da apresentação.
