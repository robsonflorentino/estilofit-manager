# Feature 015 — Meta de Vendas para o Pró-labore (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-sales-target`                  |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `report` + `settings` + tela        |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Relatórios (012), Configurações (014)       |

---

## Objetivo

Mostrar, na tela de Relatórios, quanto é preciso **vender por mês** para o dono retirar o
**pró-labore desejado** (salário configurável), comparando a **meta** de faturamento com o
**realizado**. Gráfico de barras (realizado) + linha (meta) por mês.

---

## Regra de negócio

A partir de um salário-alvo, deriva-se o faturamento necessário:

```
meta_faturamento = pró-labore desejado / (fração de lucro × PRO_LABORE_PCT / 100)
```

- **Pró-labore desejado**: nova configuração `TARGET_PRO_LABORE` (R$/mês), editável em Configurações
- **Fração de lucro**: a **real do mês** = (faturamento − custo das mercadorias vendidas) / faturamento. Meses sem vendas usam a margem padrão configurada (`DEFAULT_PROFIT_MARGIN`) convertida para fração sobre a venda: `margin / (100 + margin)`
- **PRO_LABORE_PCT**: o mesmo percentual do lucro usado no KPI do Dashboard

Exemplo: salário R$3.000, margem de lucro 50%, pró-labore 30% → meta = 3000 / (0,50 × 0,30) = **R$20.000/mês**.

---

## Backend

### Configuração
- Nova chave no catálogo `SettingKey`: `TARGET_PRO_LABORE` (decimal, ≥ 0, padrão 3000)
- Migration `V15__insert_target_pro_labore_setting.sql` semeia a chave
- `SettingsReader.targetProLabore()` para leitura

### Agregação e cálculo
- `SaleRepository.monthlyAggregate(start, end)` → faturamento e custo por ano/mês (`EXTRACT`, `GROUP BY`), apenas vendas confirmadas
- `ReportService.salesTarget(months)` — monta a série dos últimos N meses (1–24), calcula a meta por mês e o flag `achieved` (realizado ≥ meta)
- DTOs `SalesTargetMonthResponse` (mês, realizado, meta, margem %, atingido) e `SalesTargetResponse` (salário, %, série)

### Endpoint
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/reports/sales-target?months=6` | 🟡 Admin + Gestor |

---

## Frontend

- Tipos em `types/report.ts`, `reportService.salesTarget(months)`
- Nova seção no topo de `ReportsPage.tsx`: resumo do mês (meta, realizado, situação), seletor de 6/12 meses, e `ComposedChart` (Recharts) com barras do realizado + linha da meta. Link direto para `/settings` para ajustar o salário desejado
- `money()` trata `null`/`undefined` com `== null`

---

## Verificação Realizada

### Testes — 133 no total (4 novos), 0 falhas
- `ReportSalesTargetTest` (4): retorna a série com N meses e parâmetros; mês sem vendas usa margem padrão e produz meta determinística (salário 3000 → R$20.000); mês corrente reflete realizado e o flag `achieved` é coerente; vendedor → 403
- A query `EXTRACT`/`GROUP BY` foi validada contra o PostgreSQL real (Testcontainers)

### Manual (API real)
- `TARGET_PRO_LABORE` aparece nas Configurações (5 chaves) e é editável
- Salário ajustado para R$5.000: meta passou a R$33.333,33/mês (5000 / (0,50 × 0,30)) — proporcional ao aumento do salário
- Meses sem vendas usam a margem padrão (50%); mês corrente reflete o realizado com `achieved` correto
- Valor restaurado ao final; Frontend com typecheck 0 erros

---

## Notas Técnicas
- Nos testes, a asserção de valor exato usa um mês passado sem vendas (determinístico via margem padrão), já que o mês corrente é estado compartilhado no banco de teste.
- A meta fica alta quando a margem é baixa — é o comportamento correto: com menos lucro por venda, é preciso vender mais para o mesmo pró-labore.
