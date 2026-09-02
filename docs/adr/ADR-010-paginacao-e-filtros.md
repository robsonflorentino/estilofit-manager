# ADR-010 — Paginação e Filtros

## Status
Aceito

## Data
2026-09-01

## Contexto
As listagens do sistema (produtos, vendas, estoque, parcelas, etc.) podem crescer com o tempo e precisam de paginação para não sobrecarregar banco, API e frontend. Também é necessário um padrão consistente de filtros para que o frontend saiba o que esperar em cada endpoint.

## Decisão
Adotar paginação baseada em **page/size** (offset-based) com envelope de resposta padronizado, e filtros via **query parameters** tipados.

---

## 1. Paginação

### Parâmetros de Request

| Parâmetro | Tipo    | Padrão | Mínimo | Máximo | Descrição                        |
|-----------|---------|:------:|:------:|:------:|----------------------------------|
| `page`    | integer | `0`    | `0`    | —      | Índice da página (base zero)     |
| `size`    | integer | `20`   | `1`    | `100`  | Quantidade de itens por página   |
| `sort`    | string  | varia  | —      | —      | Campo e direção (ex: `name,asc`) |

### Envelope de Resposta Paginada

Todo endpoint que retorna lista paginada segue este formato:

```json
{
  "content": [ ...itens da página atual... ],
  "page": 0,
  "size": 20,
  "totalElements": 87,
  "totalPages": 5,
  "first": true,
  "last": false,
  "empty": false
}
```

| Campo           | Tipo    | Descrição                                        |
|-----------------|---------|--------------------------------------------------|
| `content`       | array   | Itens da página atual                            |
| `page`          | integer | Página atual (base zero)                         |
| `size`          | integer | Tamanho da página solicitado                     |
| `totalElements` | long    | Total de registros que atendem aos filtros        |
| `totalPages`    | integer | Total de páginas disponíveis                     |
| `first`         | boolean | Se é a primeira página                           |
| `last`          | boolean | Se é a última página                             |
| `empty`         | boolean | Se o `content` está vazio                        |

### Implementação no Backend (Spring Data)
O Spring Data já fornece `Page<T>` e `Pageable` nativamente — sem necessidade de implementação manual:

```kotlin
// Controller
@GetMapping
fun listProducts(
    @RequestParam(defaultValue = "") name: String,
    @RequestParam(required = false) categoryId: UUID?,
    @RequestParam(defaultValue = "true") active: Boolean,
    pageable: Pageable  // Spring injeta automaticamente page, size e sort
): ResponseEntity<Page<ProductSummaryResponse>> {
    return ResponseEntity.ok(productService.findAll(name, categoryId, active, pageable))
}

// Service
fun findAll(name: String, categoryId: UUID?, active: Boolean, pageable: Pageable): Page<ProductSummaryResponse> {
    return productRepository.findAllWithFilters(name, categoryId, active, pageable)
        .map { it.toSummaryResponse() }
}
```

### Ordenação Padrão por Endpoint

| Endpoint           | Ordenação padrão          |
|--------------------|---------------------------|
| `/products`        | `name,asc`                |
| `/suppliers`       | `name,asc`                |
| `/sales`           | `confirmedAt,desc`        |
| `/supply-lots`     | `receivedAt,desc`         |
| `/installments`    | `dueDate,asc`             |
| `/stock/movements` | `createdAt,desc`          |
| `/users`           | `name,asc`                |
| `/promotion-alerts`| `createdAt,desc`          |

---

## 2. Filtros

### Convenção
Filtros são sempre passados como **query parameters**. Nenhum filtro é obrigatório — a ausência de um filtro retorna todos os registros (respeitando a paginação).

Tipos de filtro suportados:

| Tipo          | Exemplo                                   | Comportamento                       |
|---------------|-------------------------------------------|-------------------------------------|
| Texto         | `?name=blusa`                             | ILIKE `%blusa%` (case-insensitive)  |
| UUID/FK       | `?categoryId=uuid`                        | Igualdade exata                     |
| Enum          | `?status=CONFIRMED`                       | Igualdade exata                     |
| Boolean       | `?active=true`                            | Igualdade exata                     |
| Data início   | `?startDate=2026-09-01`                   | `>= startDate`                      |
| Data fim      | `?endDate=2026-09-30`                     | `<= endDate`                        |
| Flag especial | `?lowStock=true`                          | Lógica específica do endpoint       |

### Filtros por Endpoint

**`GET /products`**
```
?name=blusa        → busca por nome (parcial, case-insensitive)
?categoryId=uuid   → filtra por categoria
?active=true|false → filtra por status (padrão: não filtra)
```

**`GET /suppliers`**
```
?name=moda         → busca por nome (parcial)
?active=true|false → filtra por status
```

**`GET /sales`**
```
?channelId=uuid          → filtra por canal de venda
?paymentMethod=PIX       → filtra por forma de pagamento
?status=CONFIRMED        → filtra por status
?startDate=2026-09-01    → vendas a partir desta data
?endDate=2026-09-30      → vendas até esta data
?sellerId=uuid           → filtra por vendedor (Admin/Gestor; Vendedor sempre usa o próprio id)
```

**`GET /supply-lots`**
```
?supplierId=uuid         → filtra por fornecedor
?startDate=2026-09-01    → lotes a partir desta data
?endDate=2026-09-30      → lotes até esta data
```

**`GET /stock/summary`**
```
?productId=uuid    → filtra por produto
?categoryId=uuid   → filtra por categoria
?size=M            → filtra por tamanho
?color=Azul        → filtra por cor
?lowStock=true     → apenas variações abaixo do threshold configurado
```

**`GET /stock/movements`**
```
?variantId=uuid          → filtra por variação
?type=ENTRY|SALE|ADJUSTMENT → filtra por tipo
?startDate=2026-09-01    → movimentações a partir desta data
?endDate=2026-09-30      → movimentações até esta data
```

**`GET /installments`**
```
?status=PENDING          → filtra por status da parcela
?startDueDate=2026-10-01 → parcelas com vencimento a partir desta data
?endDueDate=2026-10-31   → parcelas com vencimento até esta data
?saleId=uuid             → parcelas de uma venda específica
```

**`GET /users`**
```
?name=carlos       → busca por nome (parcial)
?role=SELLER       → filtra por perfil
?active=true|false → filtra por status
```

**`GET /promotion-alerts`**
```
?status=ACTIVE|DISMISSED|RESOLVED → filtra por status do alerta
```

---

## 3. Implementação no Frontend

### Hook Genérico de Paginação
```typescript
// src/hooks/usePagination.ts
export function usePagination(initialSize = 20) {
  const [page, setPage] = useState(0)
  const [size] = useState(initialSize)

  const reset = () => setPage(0)

  return { page, size, setPage, reset }
}
```

### Hook Genérico de Filtros
```typescript
// src/hooks/useFilters.ts
export function useFilters<T extends Record<string, unknown>>(initialFilters: T) {
  const [filters, setFilters] = useState<T>(initialFilters)

  const updateFilter = (key: keyof T, value: unknown) => {
    setFilters(prev => ({ ...prev, [key]: value }))
  }

  const resetFilters = () => setFilters(initialFilters)

  // Remove campos undefined/null/'' antes de enviar para a API
  const activeFilters = Object.fromEntries(
    Object.entries(filters).filter(([, v]) => v !== undefined && v !== null && v !== '')
  )

  return { filters, activeFilters, updateFilter, resetFilters }
}
```

### Uso Combinado
```typescript
const { page, size, setPage, reset } = usePagination()
const { filters, activeFilters, updateFilter, resetFilters } = useFilters({
  name: '',
  categoryId: undefined,
  active: undefined,
})

// Resetar paginação ao mudar filtros
const handleFilterChange = (key: string, value: unknown) => {
  updateFilter(key, value)
  reset() // volta para a página 0
}

const { data } = useQuery({
  queryKey: ['products', page, size, activeFilters],
  queryFn: () => api.get('/products', { params: { page, size, ...activeFilters } })
})
```

### Componente de Paginação
```typescript
// Exibe: "Mostrando 21–40 de 87 resultados"  ← anterior  1 2 3 4 5  próximo →
<Pagination
  page={page}
  totalPages={data?.totalPages ?? 0}
  totalElements={data?.totalElements ?? 0}
  size={size}
  onPageChange={setPage}
/>
```

---

## Justificativa
- `page/size` (offset-based) é simples, bem suportado pelo Spring Data e adequado para o volume esperado no sistema
- Cursor-based pagination seria mais eficiente para volumes muito grandes, mas desnecessário para uma loja de pequeno porte
- Filtros via query parameters são idiomáticos para REST, facilmente testáveis via Swagger e suportados nativamente pelo Spring
- Envelope de resposta consistente permite que o componente de paginação do frontend seja genérico e reutilizável
- Resetar para `page=0` ao mudar filtros é comportamento esperado pelo usuário

## Consequências
- `size` máximo de `100` evita queries pesadas no banco por acidente
- Filtros de texto usam `ILIKE` (case-insensitive) — requer atenção à performance em tabelas grandes (índice em `name` recomendado)
- O campo `sort` segue o padrão do Spring: `campo,direção` — ex: `confirmedAt,desc`
- Campos `first`, `last` e `empty` no envelope facilitam lógica de navegação no frontend sem cálculos extras
