# Feature 018 — Ranking de Vendedores (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-seller-ranking`                |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `report` + tela Relatórios          |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Relatórios (012), Vendas (010)              |

---

## Objetivo

Ranking de vendedores por **faturamento** no período, com pódio para os três primeiros
(ouro, prata, bronze) e tabela para os demais. Na tela de Relatórios (Admin + Gestor).

---

## Backend

- `SaleRepository.sellerRanking(start, end)` → por vendedor: faturamento e nº de vendas (GROUP BY), apenas confirmadas
- `ReportService.sellerRanking(startDate, endDate)` → `SellerRankingResponse` (posição, vendedor, faturamento, nº de vendas, ticket médio), ordenado por faturamento desc, posição 1..N
- Endpoint `GET /reports/seller-ranking?startDate&endDate` (Admin + Gestor)

---

## Frontend

- Tipos em `types/report.ts`, `reportService.sellerRanking(period)`
- Seção "Ranking de vendedores" na `ReportsPage`: pódio (top 3 com medalhas 🥇🥈🥉, faturamento, nº de vendas e ticket médio) + tabela do 4º lugar em diante

---

## Verificação Realizada

### Testes — 140 no total (3 novos), 0 falhas
- `ReportSellerRankingTest` (3): ranking ordenado por faturamento com ticket médio e posições sequenciais; venda cancelada não conta; vendedor → 403

### Manual (API real)
- 3 vendedores com faturamentos distintos: a lista veio ordenada por faturamento desc, posições corretas e ticket médio = faturamento / nº de vendas
- Pódio (top 3) exibe os três maiores faturamentos do período
