# 022 — Preço de venda manual (margem vira sugestão)

## Problema
O preço de venda de uma variação era **sempre derivado da margem** sobre o custo médio, e
toda entrada de mercadoria sobrescrevia esse preço. Na prática, a mesma peça vem de
fornecedores diferentes (custos diferentes) e a loja pratica um preço próprio — às vezes
acima da margem sugerida, em casos raros abaixo. Faltava poder fixar o preço manualmente.

## Solução
O cálculo por margem passa a ser um **preço sugerido**. Cada variação ganha um modo de
**preço manual** (`priceOverride`):

- **Modo automático** (padrão): o preço segue a margem; a entrada de mercadoria recalcula
  o preço a cada custo novo (comportamento anterior).
- **Modo manual**: o preço é definido pelo usuário, acima ou abaixo do sugerido. A entrada
  de mercadoria continua recalculando o **custo médio**, mas **não altera o preço manual**.
- **Voltar ao sugerido**: desliga o modo manual e recalcula o preço pela margem sobre o
  custo médio atual.

Preço abaixo do custo é **permitido** (caso raro, intencional) e apenas sinalizado
visualmente (sem bloqueio).

## Regras
- `priceOverride = true` ⇒ `SupplyLotService` não sobrescreve `salePrice` (só o `averageCost`).
- Definir `salePrice` no update liga `priceOverride`. Não altera a `profitMargin` desejada
  (ela é a base do preço sugerido e do "voltar ao sugerido").
- `resetToSuggested = true` desliga o override e recalcula `salePrice = averageCost × (1 + margem/100)`.
- Preço sugerido exposto na resposta (`suggestedPrice`), calculado com a margem da variação
  ou a global (`DEFAULT_PROFIT_MARGIN`) quando a variação não tem margem própria.
- Venda, lucro e comissão não mudam: já usam o `salePrice` gravado (snapshot na venda) e o
  `averageCost`. O lucro passa a refletir o preço realmente praticado.

## Backend
- Migration **V20**: coluna `price_override BOOLEAN NOT NULL DEFAULT FALSE` em `product_variants`.
- `ProductVariant`: campo `priceOverride`.
- `VariantResponse`: campos `priceOverride` e `suggestedPrice` (derivado).
- `UpdateVariantRequest`: campo `resetToSuggested`.
- `ProductVariantService.update`: trata preço manual (liga override) e reset (recalcula pela margem).
- `SupplyLotService.create`: só recalcula o preço quando `priceOverride == false`.
- `ProductService`/`toDetailResponse`: passam a margem global para calcular o `suggestedPrice`.

## Frontend
- `VariantsPanel`: coluna de custo e de preço com selo **"manual"**, preço **sugerido** como
  referência e aviso de preço abaixo do custo. Botão de editar preço (ícone de etiqueta) abre
  um editor com o preço manual, o aviso em tempo real e o botão **"Voltar ao sugerido"**.

## Verificação (end-to-end real)
1. Entrada custo 30 ⇒ preço automático 60 (margem 100%).
2. Preço manual 150 ⇒ `priceOverride=true`, sugerido continua 60.
3. Nova entrada custo 50 (custo médio ⇒ 40): preço permaneceu **150**, custo atualizou para
   40, sugerido recalculou para 80.
4. Voltar ao sugerido ⇒ preço recalculado para **80**, `priceOverride=false`.

158 testes de backend verdes (5 novos) + typecheck do frontend limpo.
