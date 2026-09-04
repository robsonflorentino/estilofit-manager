# 023 — Snapshot de custo na venda + correção de custo da variação

## Problema
Duas necessidades ligadas:
1. Poder **corrigir o custo médio** de uma variação (quando foi cadastrado errado numa entrada).
2. Que essa correção **não afete o lucro de vendas já realizadas**.

O sistema calculava o lucro histórico lendo o custo médio **atual** da variação
(`i.variant.averageCost`) — sem snapshot. Logo, corrigir o custo mudaria retroativamente o
lucro de todas as vendas passadas daquela peça, o que não é aceitável.

## Solução (duas partes)

### Parte 1 — Snapshot de custo na venda
- `sale_items` ganha a coluna `unit_cost`: o custo médio da variação é **congelado** no
  momento da venda (`SaleService.create` grava `variant.averageCost` do instante).
- As três queries de custo/lucro (`aggregateConfirmed`, `monthlyAggregate`, `profitByChannel`)
  passam a somar `i.unitCost` em vez de `i.variant.averageCost`.
- **Backfill** das vendas existentes na migration V21: preenche `unit_cost` com o custo médio
  atual da variação (melhor aproximação disponível, já que não havia snapshot). Da migração
  em diante, cada venda carimba o próprio custo.

Resultado: o lucro de cada venda fica fixo e não muda por alterações futuras de custo.

### Parte 2 — Correção do custo da variação
- Endpoint `POST /stock/cost-corrections` (Admin/Gestor): grava o novo `averageCost` e
  registra um `StockMovement` tipo `ADJUSTMENT` com `quantity = 0` e
  `referenceType = COST_CORRECTION`, cuja nota guarda o valor anterior, o novo e a
  justificativa (auditoria). Justificativa obrigatória (mín. 5 caracteres).
- **Não** altera a quantidade em estoque nem o preço de venda. O preço é recalculado
  naturalmente na próxima entrada de mercadoria (se a variação estiver em modo automático).
- Como o custo das vendas passadas já está congelado (Parte 1), a correção afeta apenas
  **vendas futuras** e o **valor de estoque atual**.

## Regras
- Custo aceito: `>= 0` (0 permitido, ex.: brinde/consignação).
- Só ADMIN e MANAGER corrigem custo (SELLER recebe 403).
- Vendas canceladas continuam fora dos agregados.

## Backend
- Migration **V21**: `unit_cost` em `sale_items` (nullable → backfill → NOT NULL DEFAULT 0).
- `SaleItem.unitCost`; `SaleService.create` grava o snapshot.
- `SaleRepository`: 3 queries de custo passam a usar `i.unitCost`.
- `StockService.correctCost` + DTO `CorrectCostRequest` + `POST /stock/cost-corrections`.

## Frontend
- `StockPage`: botão "Corrigir custo médio" (ícone de moedas) por variação, com modal que
  mostra o custo atual, campo do novo custo e justificativa. Aviso de que vendas já feitas
  não são afetadas.

## Verificação (end-to-end real)
Fluxo pela API (backend rodando + Postgres real):
1. Entrada custo 50 → venda de 2un: lucro **+100** (2 × (100−50)).
2. Correção de custo 50 → 80: lucro da venda anterior **não muda** (delta 0).
3. Nova venda de 1un: lucro **+20** (100−80) — usa o custo corrigido.

162 testes de backend verdes (4 novos, incluindo o teste-chave "corrigir custo não muda o
lucro de vendas já feitas") + typecheck do frontend limpo.
