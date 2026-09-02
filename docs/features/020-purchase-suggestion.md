# Feature 020 — Sugestão de Compra do Próximo Lote (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-purchase-suggestion`           |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `report` + `settings` + tela Relatórios |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Relatórios (012), Estoque (009), Vendas (010) |

---

## Objetivo

Relatório que sugere **quanto comprar** de cada variação no próximo lote, com base nas vendas
acumuladas e na posição atual de estoque. As variações mais **urgentes** (menor cobertura de
estoque) aparecem primeiro. Na tela de Relatórios (Admin + Gestor).

---

## Lógica

Para cada variação **ativa que vendeu** no período de referência:
- **Velocidade** = unidades vendidas (confirmadas) ÷ dias do período de referência
- **Cobertura** = estoque atual ÷ velocidade → quantos dias de estoque ainda restam
- **Demanda** = velocidade × horizonte-alvo (`PURCHASE_COVERAGE_DAYS`, padrão 30 dias)
- **Sugestão de compra** = arredonda pra cima `(demanda − estoque atual)`, com piso 0
- **Custo estimado** = sugestão × custo médio da variação
- **Ordenação**: menor cobertura primeiro (mais urgente)

Regras:
- Variações que **não venderam** no período não entram (o encalhe é tratado no relatório de Alertas).
- Estoque abaixo do mínimo (`LOW_STOCK_THRESHOLD`) recebe destaque.
- Período de referência configurável na consulta (30/60/90 dias; padrão 90).

---

## Backend

- `SettingKey.PURCHASE_COVERAGE_DAYS` (inteiro ≥ 1, padrão 30) — aparece em Configurações; `SettingsReader.purchaseCoverageDays()`; migration `V17` semeia
- `SaleItemRepository.salesForPurchaseSuggestion(start, end)` → por variação ativa que vendeu: qtd vendida, estoque atual, custo médio
- `ReportService.purchaseSuggestion(referenceDays)` → calcula velocidade, cobertura, sugestão e custo estimado; ordena por urgência
- Endpoint `GET /reports/purchase-suggestion?days=90` (Admin + Gestor)

---

## Frontend

- Tipos em `types/report.ts`, `reportService.purchaseSuggestion(days)`
- Seção "Sugestão de compra do próximo lote" na `ReportsPage`: seletor de período (30/60/90), tabela com produto, estoque (badge quando abaixo do mínimo), vendas no período, venda/dia, cobertura (badge quando ≤ alvo), sugestão e custo estimado

---

## Verificação Realizada

### Testes — 150 no total (5 novos), 0 falhas
- `ReportPurchaseSuggestionTest` (5): sugestão coerente com velocidade e cobertura (estoque 30, vende 2/dia, cobertura 15 dias → sugere 30 para 30 dias); variação sem vendas não aparece; estoque farto → sugestão 0; parâmetros na resposta; vendedor → 403

### Manual (API real, base populada)
- Cenário controlado: estoque 15, vendeu 45 em 30 dias → velocidade 1,5/dia, cobertura 10 dias, **sugestão 30 un**, custo estimado R$1.200, e apareceu em 1º (mais urgente)
- Itens com estoque de sobra (200 un, giro baixo) corretamente sugerem 0
- Frontend: typecheck 0 erros

---

## Notas Técnicas
- Como o `confirmedAt` das vendas é sempre "agora", períodos de referência maiores diluem a velocidade; em produção com histórico real isso se ajusta naturalmente.
- A cobertura vem nula quando a velocidade é ~0 (sem giro relevante) e esses itens vão para o fim da lista.
