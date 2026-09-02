# Feature 014 — Configurações (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-settings`                      |
| Data          | 2026-09-02                                  |
| Módulos       | Backend `settings` + tela no frontend       |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | (tabela `system_settings` — V11)            |

---

## Objetivo

Tela `/settings` (Admin) para ler e editar os parâmetros do sistema pela interface,
substituindo a leitura "crua" do `SettingsReader` por um serviço de configurações completo
(leitura + escrita), mantendo o fallback para valores padrão.

---

## Chaves gerenciadas (catálogo fixo)

| Chave | Rótulo | Tipo | Faixa | Padrão |
|-------|--------|------|-------|--------|
| `DEFAULT_PROFIT_MARGIN` | Margem de lucro padrão (%) | decimal | ≥ 0 | 100 |
| `LOW_STOCK_THRESHOLD` | Alerta de estoque baixo (un) | inteiro | ≥ 0 | 2 |
| `PRO_LABORE_PCT` | Percentual de pró-labore (%) | decimal | 0–100 | 30 |
| `PROMOTION_ALERT_DAYS` | Dias sem venda para alerta | inteiro | ≥ 1 | 60 |

Chaves livres não são permitidas — o catálogo é fixo no código (`SettingKey`), o que garante
validação de tipo/faixa e edição segura.

---

## Backend (`settings/`)

### Componentes
- `domain/SystemSetting` — entidade da tabela `system_settings` (chave, valor, descrição, `updatedBy`, `updatedAt`)
- `domain/SettingKey` — catálogo das chaves com rótulo, tipo (`INTEGER`/`DECIMAL`), faixa e fallback
- `repository/SystemSettingRepository` — `findByKey`, `findAllByOrderByKeyAsc`
- `service/SettingsService` — `list()` (mescla catálogo + banco), `rawValue(key)` (para o leitor), `update()` com validação por tipo/faixa e registro de `updatedBy`
- `controller/SettingsController` — `GET /settings`, `PUT /settings/{key}` (Admin)
- `dto/SettingDTOs` — `SettingResponse` (metadados de edição), `UpdateSettingRequest`

### Refatoração do `SettingsReader`
O `SettingsReader` (usado por Estoque, Entrada de Lote, Dashboard e Promoções) passou a
**delegar ao `SettingsService`** em vez de ler o banco diretamente com `EntityManager`. Os
métodos públicos (`defaultProfitMargin`, `lowStockThreshold`, `proLaborePct`, `promotionAlertDays`)
e o comportamento de fallback foram preservados — nenhum consumidor precisou mudar. A suíte
completa (incluindo esses 4 módulos) permaneceu verde após a mudança.

### Validação no update
- Valor não numérico, inteiro com casas decimais, ou fora da faixa → 422 (`BusinessException`)
- Chave fora do catálogo → 404
- Valor normalizado antes de gravar (inteiros sem casas decimais)

### Endpoints
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/settings` | 🔴 Admin |
| PUT | `/settings/{key}` | 🔴 Admin |

---

## Frontend

- `types/settings.ts`, `services/settingsService.ts`
- `pages/SettingsPage.tsx` — um card por chave com rótulo, descrição, quem alterou por último, input numérico adequado (step por tipo), validação client-side (tipo/faixa) e botão salvar por chave (habilitado só quando o valor muda e é válido), com toast
- Rota `/settings` (RoleRoute Admin); o item de menu ("Configurações") já existia

---

## Verificação Realizada

### Testes — 129 no total (8 novos), 0 falhas
- `SettingsIntegrationTest` (8): admin lista as 4 chaves com metadados; PUT altera e persiste (releitura confirma) e registra quem alterou; inteiro com casas decimais → 422; pró-labore > 100 → 422; valor não numérico → 422; chave desconhecida → 404; gestor → 403 (GET e PUT); sem token → 401
- Os módulos que consomem o `SettingsReader` refatorado (Estoque, Dashboard, Promoções) continuaram verdes — confirmando que a refatoração foi segura

### Manual (API real)
- GET lista as 4 chaves com valores e faixas corretas
- PUT `LOW_STOCK_THRESHOLD` 2 → 5: 200, releitura confirma 5, `updatedByName` = "Administrador"
- PUT `PRO_LABORE_PCT=150` → 422; PUT chave inexistente → 404
- Gestor → 403 no GET e no PUT
- Valor restaurado ao final; Frontend com typecheck 0 erros

---

## Notas Técnicas
- Nos testes, o update de persistência usa `PROMOTION_ALERT_DAYS` (os testes de Promoção passam `days` explícito no request, não dependem do valor global), evitando interferência entre casos no banco compartilhado.
- Com esta feature, o `SettingsService` é a fonte única de configurações; o antigo comentário "enquanto o módulo de Settings não é implementado" (decisão 3 da feature 009) foi resolvido.

---

## Estado do Projeto
Todos os itens do menu estão implementados e verificados: Dashboard, Categorias, Produtos,
Estoque, Fornecedores, Vendas, Contas a Receber, Relatórios, Alertas, Usuários e Configurações.
