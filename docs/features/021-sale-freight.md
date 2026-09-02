# Feature 021 — Frete na Venda (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-sale-freight`                  |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `sale` + telas (Nova Venda, Vendas) |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Vendas (010)                                |

---

## Objetivo

Permitir registrar frete na venda com três opções, tratando o frete como **repasse**
(ex.: entregador de app) — não é receita nem lucro da loja, apenas soma ao total que o
cliente paga.

---

## Regras

Três opções de frete:
- **Sem Frete** (`NONE`, padrão) — frete zero
- **Frete Grátis** (`FREE`) — a loja não cobra; valor zero
- **Frete** (`PAID`) — cobrado do cliente; **libera o campo de valor** (ex.: R$25,00)

Princípio central — o frete **não contamina nenhum cálculo**:
- `finalAmount` continua sendo **apenas o valor dos produtos** (o que alimenta todos os relatórios)
- Frete fica em campo separado (`freightAmount`); o **total pago** pelo cliente = `finalAmount + freightAmount`
- Frete **não entra** em: faturamento, lucro, pró-labore, meta, comissão, lucro por canal
- Comissão do vendedor incide só sobre os produtos; parcelas do cartão são sobre os produtos

---

## Backend

- Enum `FreightType` (`NONE`/`FREE`/`PAID`); campos `freightType`/`freightAmount` em `Sale`
- Migration `V18`: colunas em `sales` (default `NONE`/0) + CHECKs de tipo e valor ≥ 0
- `CreateSaleRequest` ganhou `freightType` e `freightAmount`
- `SaleService.create` valida: `PAID` exige valor > 0 (senão 422); `NONE`/`FREE` forçam zero
- `SaleDetailResponse` expõe `freightType`, `freightAmount` e `totalPaid` (`finalAmount + freightAmount`, apenas para exibição)

---

## Frontend

- Tipos e labels em `types/sale.ts` (`FreightType`, `FREIGHT_TYPE_LABELS`)
- `NovaVendaPage`: seletor de frete (Sem Frete / Frete Grátis / Frete); a opção "Frete" libera o campo de valor. Resumo mostra Produtos, Frete e **Total a pagar**; confirmar exige valor > 0 quando "Frete"
- `SalesPage`: detalhe da venda mostra o frete e o total pago quando aplicável

---

## Verificação Realizada

### Testes — 153 no total (3 novos), 0 falhas
- `SaleFreightTest` (3): venda `PAID` grava frete e `totalPaid = finalAmount + frete`, sem alterar o faturamento (produtos); `FREE` e `NONE` ficam com frete zero; `PAID` sem valor → 422
- Nenhum teste de relatório/comissão quebrou — o frete não entra nos cálculos

### Manual (API real)
- Venda com 2 un (produtos R$200) + frete R$25: `finalAmount=200`, `freightAmount=25`, `totalPaid=225`
- Venda de vendedor com frete: **comissão R$10** (5% de R$200, não sobre o frete) — confirma o isolamento
- Frontend: typecheck 0 erros

---

## Notas Técnicas
- Manter `finalAmount` = só produtos (em vez de somar o frete e subtrair depois) foi a decisão que preservou todos os relatórios existentes intactos, sem risco de contaminação.
