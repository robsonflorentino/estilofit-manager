# Tech Design — Feature 009: Estoque / Entrada de Mercadoria

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/api-inventory`                |
| Status        | ✅ Design aprovado (2026-09-01) — todas as recomendações aceitas |
| Depende de    | Produtos/Variações (007), Fornecedores (008) |
| Regras        | RN-008 a RN-015                        |

> Documento de **design prévio** — decisões a validar **antes** de codar.

---

## 1. Objetivo

Registrar entradas de mercadoria (lotes) vindas de fornecedores. Ao confirmar um lote, o sistema:
1. Rateia o frete entre os itens (por valor)
2. Calcula o custo real de cada item (custo + frete rateado)
3. Atualiza o custo médio ponderado da variação
4. Recalcula o preço de venda pela margem
5. Incrementa o estoque
6. Registra as movimentações

Tudo de forma **atômica** (uma transação).

---

## 2. Modelo (tabelas já existem — migrations V5 e V6)

```
supply_lots                    supply_lot_items
├── id                         ├── id
├── supplier_id (FK)           ├── lot_id (FK)
├── received_at                ├── variant_id (FK)
├── freight_cost               ├── quantity
├── total_cost                 ├── unit_cost        (sem frete)
├── notes                      ├── freight_share    (frete rateado do item)
├── created_by (FK user)       └── real_unit_cost   (custo + frete/qtd)
└── created_at

stock_movements
├── id, variant_id (FK), type (ENTRY/SALE/ADJUSTMENT)
├── quantity, reference_type, reference_id
├── notes, user_id (FK), created_at
```

Nenhuma migration nova. O design foca na lógica transacional.

---

## 3. Fluxo do Registro de Lote (POST /supply-lots)

```
1. Valida fornecedor (existe e ativo?)
2. Valida cada variação (existe e ativa?)
3. Calcula custo_total_sem_frete = Σ (unit_cost × quantity)
4. Para cada item:
     participacao   = (unit_cost × quantity) / custo_total_sem_frete
     freight_share  = freight_cost × participacao
     real_unit_cost = unit_cost + (freight_share / quantity)
5. Para cada variação:
     novo_custo_medio = custo médio ponderado (RN-010)
     novo_preco_venda = custo_medio × (1 + margem/100)   [margem da variante ou global]
     stock_quantity  += quantity
6. Persiste lote + itens
7. Registra StockMovement (ENTRY) por item
8. total_cost = custo_total_sem_frete + freight_cost
Tudo em @Transactional.
```

---

## 4. Pontos que preciso que você decida

### 4.1. Frete zero
Se `freight_cost = 0`, o rateio é trivial (freight_share = 0, real_unit_cost = unit_cost). Sem decisão necessária — só confirmando que **frete zero é permitido**. OK?

### 4.2. Custo total zero (proteção contra divisão por zero)
Se todos os `unit_cost` forem 0 (improvável, mas possível), a divisão do rateio quebra.
- **Proposta:** exigir `unit_cost > 0` na validação (já está no CreateVariantRequest do lote). Assim `custo_total_sem_frete` nunca é zero quando há itens. OK?

### 4.3. Margem para recalcular o preço de venda
Ao atualizar o custo, recalculo o `sale_price` pela margem. Qual margem usar?
- Se a variação tem `profit_margin` própria → usa ela
- Se é `null` → usa a **margem global** (`DEFAULT_PROFIT_MARGIN` das configurações, padrão 100%)
- **Decisão:** confirmar que leio a margem global de `system_settings`. (Ainda não temos o módulo de Settings implementado — proponho ler direto da tabela `system_settings` por enquanto, com fallback 100% se a chave não existir. OK?)

### 4.4. Preço de venda ajustado manualmente
A RN-006 permite que o gestor fixe um `sale_price` manual. Se ele fez isso e depois entra um novo lote (novo custo), **devo sobrescrever o preço manual** pelo recalculado?
- **Proposta:** o recálculo automático **sempre** atualiza o `sale_price` com base na margem. Se o gestor quer um preço fixo, ele reajusta depois pela tela de variação. (Alternativa: só recalcular se a variação nunca teve preço manual — mais complexo, exige um flag.)
- **Minha recomendação:** recalcular sempre (simples e previsível). O gestor vê o preço sugerido e ajusta se quiser. OK?

### 4.5. Edição/exclusão de lote
Um lote já confirmado alterou custo médio e estoque. Permitir **editar ou excluir** um lote é complexo (teria que reverter cálculos).
- **Proposta para esta feature:** lote é **imutável** após criação. Sem edição nem exclusão. Correção de erro se faz via ajuste manual de estoque (RN-014) — que já está no escopo.
- **Minha recomendação:** imutável nesta feature. OK?

### 4.6. Endpoints incluídos
Proponho entregar nesta feature:
- `POST /supply-lots` — registrar lote (Admin + Gestor)
- `GET /supply-lots` — listar lotes paginado (Admin + Gestor)
- `GET /supply-lots/{id}` — detalhe do lote com itens
- `GET /stock/summary` — resumo de estoque por variação (todos os perfis) — RN-032
- `GET /stock/movements` — histórico de movimentações (Admin + Gestor)
- `POST /stock/adjustments` — ajuste manual (Admin + Gestor, justificativa obrigatória) — RN-014

Isso cobre entrada de mercadoria + consulta de estoque + ajuste manual num pacote coeso. Concorda com esse escopo, ou prefere dividir (ex: lotes numa feature, ajustes/consulta em outra)?

---

## 5. Estrutura de Código Proposta

```
inventory/
├── domain/
│   ├── SupplyLot.kt
│   ├── SupplyLotItem.kt
│   ├── StockMovement.kt
│   └── StockMovementType.kt (enum)
├── repository/
│   ├── SupplyLotRepository.kt
│   └── StockMovementRepository.kt
├── dto/
│   ├── SupplyLotDTOs.kt
│   └── StockDTOs.kt
├── service/
│   ├── FreightAllocator.kt      ← lógica de rateio isolada e testável
│   ├── SupplyLotService.kt      ← registro de lote (transacional)
│   └── StockService.kt          ← consulta e ajuste manual
└── controller/
    ├── SupplyLotController.kt
    └── StockController.kt
```

O `FreightAllocator` fica isolado (como o `SkuGenerator`) por concentrar a lógica de cálculo não-trivial — fácil de testar unitariamente com vários cenários.

---

## 6. Decisões a Confirmar (resumo)

1. Frete zero permitido? (sim)
2. Exigir `unit_cost > 0` para evitar divisão por zero? (sim)
3. Margem: variante ou global de `system_settings` com fallback 100%? (sim)
4. Recalcular `sale_price` sempre no novo lote, sobrescrevendo preço manual? (recomendo sim)
5. Lote imutável (sem editar/excluir; correção via ajuste manual)? (recomendo sim)
6. Escopo dos 6 endpoints num pacote só? (recomendo sim)

Responda os 6 pontos (ou "segue as recomendações") e eu parto para a implementação com testes.
