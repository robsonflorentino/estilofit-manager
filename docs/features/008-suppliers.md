# Feature 008 — Fornecedores (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-suppliers`                     |
| Data          | 2026-09-01                                  |
| Módulos       | Backend `supplier` + tela no frontend       |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Feature 001 (auth), Feature 005 (componentes base) |
| Regras        | RN-007                                      |

---

## Objetivo

CRUD de fornecedores. Pré-requisito para o módulo de Estoque (todo lote de entrada vem de um fornecedor). CRUD simples — tech design dispensado por acordo.

---

## Backend (`supplier/`)

- `domain/Supplier` — name, contato (telefone, email, whatsapp), cnpj (único), endereço, notas, active
- `repository/SupplierRepository` — filtros (name, active) + verificação de CNPJ duplicado
- `service/SupplierService` — CRUD com validação de CNPJ e normalização de campos vazios para null
- `controller/SupplierController` — `@PreAuthorize` a nível de classe (Admin + Gestor)
- DTOs de request/response

### Endpoints (todos 🟡 Admin + Gestor)
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/suppliers` | Listagem paginada (filtros name, active) |
| GET | `/suppliers/{id}` | Detalhe |
| POST | `/suppliers` | Criar |
| PUT | `/suppliers/{id}` | Atualizar |
| PATCH | `/suppliers/{id}/status` | Ativar/desativar |

### Regras
- CNPJ único — duplicado retorna 409 (opcional; fornecedor pode não ter CNPJ)
- Campos de contato vazios são normalizados para `null`
- Soft delete via `active`

---

## Frontend

- `types/supplier.ts`, `services/supplierService.ts`
- `pages/SuppliersPage.tsx` — listagem paginada, filtros (nome, status), modal criar/editar com todos os campos, ativar/desativar
- Rota `/suppliers` protegida (Admin + Gestor); item de menu já existia

---

## Verificação Realizada

### Testes — 75 no total (13 novos), 0 falhas
- `SupplierServiceTest` (6): create sem CNPJ, CNPJ duplicado, normalização de vazios, update not found, update CNPJ duplicado, updateStatus
- `SupplierIntegrationTest` (7): 401 sem token, 403 vendedor, listagem, criar 201, CNPJ duplicado 409, validação 400, atualizar+desativar

### Manual (API real)
- Listar 200, criar 201, CNPJ duplicado 409, vendedor 403
- SuppliersPage compila no Vite; typecheck 0 erros

---

## Próxima Feature
**Estoque / Entrada de Mercadoria** — o módulo mais complexo até aqui (lotes, frete rateado, custo médio ponderado, atualização de estoque e preço de venda por margem). Merece **tech design** antes da implementação.
