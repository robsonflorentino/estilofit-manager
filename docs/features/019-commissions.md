# Feature 019 — Comissão do Vendedor (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-commissions`                   |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `commission` + `sale` + `settings` + telas |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Vendas (010), Configurações (014), Relatórios (012) |

---

## Objetivo

Comissão do vendedor configurável (padrão 5%) sobre o **faturamento** da venda, gravada como
**snapshot** no momento da venda para preservar o histórico quando a taxa mudar. A comissão
compõe o **custo efetivo da venda** (reduz o lucro nos relatórios) e há uma tela de
**comissões a pagar** por vendedor.

---

## Decisões

- **Taxa global** configurável (`SELLER_COMMISSION_PCT`, default 5%), não por vendedor.
- **Snapshot por venda**: grava `commissionPct` e `commissionAmount` no momento da venda. Mudar a taxa depois **não** altera vendas passadas (integridade do que foi/será pago).
- Só vendas de usuários com papel **SELLER** geram comissão; vendas de Admin/Gestor gravam comissão zero. Como é snapshot, o papel no momento da venda fica implícito.
- Comissão sobre o **faturamento** (`finalAmount`), não sobre o lucro.
- Venda **cancelada** não paga (o filtro por status confirmado já resolve).
- **Não** altera o `averageCost` da variação em estoque — a comissão é custo da venda, não da aquisição.

---

## Backend

### Configuração e snapshot
- `SettingKey.SELLER_COMMISSION_PCT` (decimal 0–100, padrão 5) — aparece na tela de Configurações; `SettingsReader.sellerCommissionPct()`
- `Sale` ganhou `commissionPct` e `commissionAmount`
- `SaleService.create` grava o snapshot: taxa vigente se o vendedor é SELLER (senão 0); valor = taxa × `finalAmount`
- Migration `V16`: seed da chave + colunas em `sales` (default 0) + **backfill** (vendas confirmadas de SELLER recebem 5% retroativo)

### Impacto no lucro
As agregações de lucro passaram a somar a comissão como custo:
- `SaleRepository.aggregateConfirmed`, `monthlyAggregate` e `profitByChannel` incluem `SUM(commissionAmount)`
- Lucro = faturamento − custo das mercadorias − **comissão**, refletido em: pró-labore do Dashboard, lucro estimado dos Relatórios, lucro por canal e a margem usada na meta de vendas

### Comissões a pagar
- `SaleRepository.commissionBySeller(start, end)` (GROUP BY vendedor, `HAVING SUM(commission) > 0`)
- Módulo `commission`: `CommissionService.report()` + `CommissionController` → `GET /commissions?startDate&endDate` (Admin + Gestor)

---

## Frontend

- `types/commission.ts`, `services/commissionService.ts`
- `pages/CommissionsPage.tsx` — tela `/commissions` (Admin + Gestor): seletor de período, total a pagar e tabela por vendedor (faturamento, nº de vendas, comissão). Novo item de menu "Comissões"
- Detalhe da venda (`SalesPage`) mostra a comissão quando > 0
- Configurações passa a listar "Comissão do vendedor (%)"

---

## Verificação Realizada

### Testes — 145 no total (5 novos), 0 falhas
- `CommissionIntegrationTest` (5): venda de SELLER grava 5% (R$20 em R$400); venda de gestor grava 0; relatório soma por vendedor com total; venda cancelada não paga; vendedor → 403
- Nenhum teste anterior de lucro quebrou (usavam vendas de MANAGER, comissão 0)

### Manual (API real)
- Venda de vendedor R$400 → `commissionPct=5`, `commissionAmount=20`; venda de admin → 0
- Tela de comissões: soma por vendedor (total R$150 no cenário), admin não aparece
- Backfill aplicado: vendas antigas de vendedores já constam com 5%
- Lucro/pró-labore descontam a comissão: pró-labore R$668,62 = 30% do lucro R$2.228,74 (consistente)

---

## Notas Técnicas
- A taxa é snapshot: alterar `SELLER_COMMISSION_PCT` afeta apenas vendas futuras; as antigas mantêm o valor gravado.
- Evolução possível: comissão personalizada por vendedor (campo opcional no usuário que sobrescreve a global) — não implementado por ora.
