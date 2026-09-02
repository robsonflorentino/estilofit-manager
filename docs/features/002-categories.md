# Feature 002 — Categorias

| Campo         | Valor                                |
|---------------|--------------------------------------|
| Branch        | `feature/api-categories`             |
| Data          | 2026-09-01                           |
| Módulos       | `category`                           |
| Status        | ✅ Concluída e verificada end-to-end |
| Depende de    | Feature 001 (auth + usuários)        |

---

## Objetivo

Implementar o CRUD de categorias de produto. É o primeiro cadastro de negócio e pré-requisito para o módulo de produtos (todo produto pertence a uma categoria).

---

## Escopo Entregue

Módulo `category/` completo, seguindo o padrão em camadas do projeto:

- `domain/Category` — entity (name único, active), herda de `BaseEntity`
- `repository/CategoryRepository` — consultas por status e verificação de duplicidade case-insensitive
- `dto/CategoryDTOs` — `CategoryResponse`, `CategoryRequest`, `CategoryStatusRequest`
- `service/CategoryService` — regras de listagem, criação, renomeação e ativação/desativação
- `controller/CategoryController` — endpoints REST com controle de acesso

A tabela `categories` já existia (migration V2) com 8 categorias seed (migration V12).

---

## Endpoints Entregues

| Método | Rota                       | Permissão          | Descrição                          |
|--------|----------------------------|--------------------|------------------------------------|
| GET    | `/categories`              | 🟢 Todos           | Lista categorias (param `onlyActive`, padrão `true`) |
| POST   | `/categories`              | 🟡 Admin + Gestor  | Cria categoria                     |
| PUT    | `/categories/{id}`         | 🟡 Admin + Gestor  | Renomeia categoria                 |
| PATCH  | `/categories/{id}/status`  | 🟡 Admin + Gestor  | Ativa/desativa categoria           |

### Exemplos

**Criar:**
```json
POST /categories
{ "name": "Macacões" }
→ 201 { "id": "uuid", "name": "Macacões", "active": true }
```

**Listar (só ativas por padrão):**
```
GET /categories              → só categorias ativas
GET /categories?onlyActive=false → todas, incluindo inativas
```

---

## Decisões Técnicas Relevantes

1. **Duplicidade case-insensitive** — `existsByNameIgnoreCase` impede cadastrar "Blusas" e "blusas"
   como categorias diferentes. Retorna 409 (`DataConflictException`).

2. **Soft delete via `active`** — categorias não são apagadas, apenas desativadas. Preserva a
   integridade referencial com produtos que já usam a categoria. A listagem padrão esconde inativas.

3. **`name` normalizado com `trim()`** — remove espaços nas pontas antes de salvar e validar,
   evitando duplicatas por espaço acidental.

4. **Sem paginação** — o volume de categorias é pequeno (dezenas no máximo), então a listagem
   retorna todas de uma vez, ordenadas por nome. Diferente de produtos/vendas, que são paginados.

---

## Como Testar

Pré-requisitos: banco e app rodando (ver Feature 001), autenticado como admin.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estilofit.com.br","password":"admin@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Listar
curl http://localhost:8080/api/v1/categories -H "Authorization: Bearer $TOKEN"

# Criar
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Macacões"}'
```

---

## Verificação Realizada

Testado contra a aplicação rodando com PostgreSQL real:

| Cenário                                          | Esperado           | Status |
|--------------------------------------------------|--------------------|:------:|
| `GET /categories` lista as 8 seed ordenadas      | 200                | ✅     |
| Criar categoria nova                             | 201                | ✅     |
| Criar duplicada (case-insensitive)              | 409                | ✅     |
| Renomear categoria                               | 200                | ✅     |
| Desativar categoria                              | 200, `active=false`| ✅     |
| Lista padrão esconde inativas                    | 8 ativas           | ✅     |
| `onlyActive=false` mostra inativas               | 9 no total         | ✅     |
| Validação com nome vazio                         | 400 + fieldErrors  | ✅     |
| Vendedor tentando criar categoria                | 403                | ✅     |

---

## Próxima Feature
`feature/api-products-variants` — Produtos + variações com geração automática de SKU.
