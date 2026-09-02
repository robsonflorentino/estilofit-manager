# Feature 009 — Estoque / Entrada de Mercadoria (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-inventory`                     |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `inventory` + telas no frontend     |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Produtos/Variações (007), Fornecedores (008) |
| Design        | `docs/features/009-inventory-design.md`     |
| Regras        | RN-008 a RN-015                             |

---

## Objetivo

Registrar entradas de mercadoria (lotes), calculando frete rateado, custo médio ponderado, preço de venda por margem e atualização de estoque — tudo atômico. Inclui consulta de estoque, histórico de movimentações e ajuste manual.

---

## Backend (`inventory/`)

### Componentes
- `domain/`: `SupplyLot`, `SupplyLotItem`, `StockMovement`, `StockMovementType`
- `repository/`: `SupplyLotRepository`, `StockMovementRepository` (+ `findStockSummary` no ProductVariantRepository)
- `service/FreightAllocator` — rateio de frete isolado e testável
- `service/SettingsReader` — lê margem/threshold de `system_settings` (fallback)
- `service/SupplyLotService` — registro transacional do lote
- `service/StockService` — resumo, movimentações, ajuste manual
- `controller/SupplyLotController`, `controller/StockController`

### Fluxo do registro de lote (atômico)
1. Valida fornecedor ativo e variações ativas
2. Rateia o frete por valor (`FreightAllocator`)
3. Calcula custo real de cada item (custo + frete/qtd)
4. Atualiza custo médio ponderado da variação (RN-010)
5. Recalcula preço de venda pela margem (variação ou global; RN-006)
6. Incrementa estoque (RN-011)
7. Registra movimentação `ENTRY` por item

### Endpoints
| Método | Rota | Permissão |
|--------|------|-----------|
| POST | `/supply-lots` | 🟡 Admin + Gestor |
| GET | `/supply-lots` | 🟡 Admin + Gestor |
| GET | `/supply-lots/{id}` | 🟡 Admin + Gestor |
| GET | `/stock/summary` | 🟢 Todos |
| GET | `/stock/movements` | 🟡 Admin + Gestor |
| POST | `/stock/adjustments` | 🟡 Admin + Gestor |

### Regras aplicadas
- Frete rateado por valor; último item absorve resíduo de arredondamento
- Custo médio ponderado quando a variação já tinha estoque
- Preço recalculado sempre (sobrescreve preço manual — decisão 4 do design)
- Lote imutável (correção via ajuste manual — decisão 5)
- Ajuste não permite estoque negativo (RN-015); justificativa obrigatória

---

## Frontend

- `types/inventory.ts`, `services/inventoryService.ts`
- `pages/SupplyEntryPage.tsx` — formulário de entrada: fornecedor, data, frete, e montagem de itens (busca produto → variação → qtd/custo), com totais
- `pages/StockPage.tsx` — resumo de estoque por variação, filtros (categoria, estoque baixo), badges de alerta, ajuste manual (modal), botão de entrada de mercadoria
- Rotas: `/stock` (todos os perfis), `/stock/entry` (Admin + Gestor)

---

## Verificação Realizada

### Testes — 86 no total (11 novos), 0 falhas
- `FreightAllocatorTest` (5): exemplo exato da RN-009, frete zero, resíduo no último item, erros de borda
- `InventoryIntegrationTest` (6): lote atualiza estoque/custo/preço, custo médio ponderado, ajuste negativo além do estoque (422), ajuste sem justificativa (400), RBAC vendedor (403), sem token (401)

### Manual (API real) — fluxo completo validado
- Lote: 201, `realUnitCost=50` (30 + 200/10), `totalCost=500`
- Variação após lote: estoque **10**, custo médio **50**, preço **100** (margem 100%)
- Ajuste válido (201), ajuste além do estoque (422)
- Movimentações: `ENTRY 10` + `ADJUSTMENT -3`
- Telas compilam no Vite; typecheck 0 erros

---

## Notas Técnicas
- `SettingsReader` lê `system_settings` via native query enquanto o módulo de Settings não existe (decisão 3). Migrar para o service de Settings quando ele for implementado.
- Fix de tipagem no form de ajuste (frontend): `quantity` tratado como string no schema zod (evita o `unknown` gerado por `z.coerce.number`), convertido na mutation.

---

## Próximos Passos
Com estoque e preços preenchidos, o próximo módulo é **Vendas** — que consome o estoque (saída), valida disponibilidade (RN-015/020), registra itens e gera parcelas (contas a receber). É complexo e merece **tech design**.
