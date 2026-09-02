# Feature 013 — Alertas de Promoção (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-promotions`                    |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `promotion` + tela no frontend      |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Vendas (010), Estoque (009)                 |

---

## Objetivo

Tela `/promotions` (Admin + Gestor) que lista variações **paradas** — com estoque e sem
venda há mais de X dias — para o lojista decidir promoções. É informativa: não altera preços.
X vem de `PROMOTION_ALERT_DAYS` (config, default 60) e pode ser ajustado na tela.

---

## Regra de "parada"

Uma variação entra no alerta quando:
- Está **ativa** e tem **estoque > 0**
- A **última venda confirmada** foi há mais de X dias — **ou nunca vendeu**

Referência dos "dias parada":
- Se já vendeu: a data da última venda confirmada (`lastSaleAt`)
- Se nunca vendeu: a data da **primeira entrada em estoque** (movimentação `ENTRY`), com fallback para a data de criação da variação

Cada linha traz: SKU, produto, tamanho/cor, estoque, preço, custo médio, última venda (ou "Nunca vendeu"), dias parada e capital parado do item (estoque × custo médio).

---

## Backend (`promotion/`)

### Componentes
- `dto/PromotionDTOs` — `StaleProductResponse` (com `daysStale`, `neverSold`, `stockValue`) e `StalePromotionResponse` (`thresholdDays`, `staleCount`, `totalStockValue`, `items`)
- `service/PromotionService` — calcula `daysStale`, filtra por limiar, ordena pelas mais paradas e soma o capital parado
- `controller/PromotionController` — `GET /promotions/stale?days=` (Admin + Gestor)

### Query
`ProductVariantRepository.findStaleCandidates()` retorna as variações ativas com estoque, cada uma com:
- `lastSaleAt` — subquery `MAX(confirmedAt)` de vendas confirmadas da variação (nulo se nunca vendeu)
- `firstEntryAt` — subquery `MIN(createdAt)` das movimentações `ENTRY` da variação

O filtro por dias e o cálculo do capital parado são feitos no service (mais legível que SQL).

### Endpoint
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/promotions/stale?days=` | 🟡 Admin + Gestor (default = `PROMOTION_ALERT_DAYS`) |

---

## Frontend

- `types/promotion.ts`, `services/promotionService.ts`
- `pages/PromotionsPage.tsx` — filtro de "sem venda há (dias)" (vazio usa o padrão do backend), três cards de resumo (variações paradas, capital parado, limiar aplicado) e tabela com badge de dias parada (amarelo, vermelho a partir de 90 dias). `money()` trata `null`/`undefined` com `== null`
- Rota `/promotions` (RoleRoute Admin + Gestor); o item de menu ("Alertas") já existia

---

## Verificação Realizada

### Testes — 121 no total (7 novos), 0 falhas
- `PromotionIntegrationTest` (7): variação com estoque que nunca vendeu aparece com `days=0` (`neverSold=true`, `lastSaleAt` nulo, `stockValue` correto); variação vendida hoje não aparece com `days=1`; variação sem estoque não aparece; limiar de 60 exclui entrada recente; resumo traz `thresholdDays`/`staleCount`/`totalStockValue`; vendedor → 403; sem token → 401
- As subqueries correlacionadas (`MIN(ENTRY.createdAt)`, `MAX(confirmedAt)` de vendas confirmadas) foram validadas contra o PostgreSQL real (Testcontainers)

### Manual (API real)
- Variação encalhada (estoque 8, nunca vendida): aparece com `neverSold=true`, `lastSaleAt=null`, capital parado R$200
- Variação vendida hoje: aparece com `neverSold=false` e `lastSaleAt` de hoje; com `days=1` nenhuma das duas aparece (ambas criadas hoje, 0 dias)
- Resumo consolida `staleCount` e capital parado total
- Vendedor → 403
- Frontend: typecheck 0 erros

---

## Notas Técnicas
- Como `confirmedAt` e as entradas são sempre "agora" no banco de teste, os cenários de valor exato foram validados com `days=0`/`days=1` (a lógica de limiar e o flag `neverSold` são determinísticos); a passagem de tempo real não é simulável via API.
- `SettingsReader` segue a decisão 3 da feature 009 (leitura direta de `system_settings`). `PROMOTION_ALERT_DAYS` já era semeado na V12.

---

## Próximos Passos
Todos os itens do menu estão implementados. Um próximo passo natural seria o módulo de
**Configurações** (`/settings`, Admin) para editar as chaves de `system_settings`
(margem padrão, limiar de estoque baixo, % de pró-labore, dias de alerta de promoção) pela
interface, substituindo o `SettingsReader` por um serviço de settings completo.
