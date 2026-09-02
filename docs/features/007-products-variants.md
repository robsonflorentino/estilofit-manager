# Feature 007 — Produtos + Variações (Backend + Frontend)

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-products-variants`             |
| Data          | 2026-09-01                                  |
| Módulos       | Backend `product` + telas no frontend       |
| Status        | ✅ Concluída e verificada                   |
| Depende de    | Feature 002 (categorias), 004 (scaffold web), 005 (componentes base) |
| Design        | `docs/features/003-products-variants-design.md` |
| Regras        | RN-001 a RN-006                             |

---

## Objetivo

Implementar o núcleo do sistema: produtos e suas variações (tamanho + cor), com **geração automática de SKU**. Backend com testes e tela no frontend.

---

## Backend

### Estrutura (`product/`)
- `domain/Product`, `domain/ProductVariant`
- `repository/ProductRepository` (filtros + `@EntityGraph` para carregar variações na listagem), `repository/ProductVariantRepository`
- `service/SkuGenerator` — geração de SKU isolada e testável
- `service/ProductService`, `service/ProductVariantService`
- `controller/ProductController`, `controller/ProductVariantController`
- DTOs de produto e variação

### Geração de SKU
Formato: `{PREFIXO_CATEGORIA}-{SEQUENCIAL}-{TAMANHO}-{COR}` → ex: `BLU-001-M-AZU`
- Prefixo: 3 primeiras letras da categoria, sem acento
- Sequencial: por prefixo, 3 dígitos
- Tamanho: maiúsculo
- Cor: 3 primeiras letras, sem acento
- Colisão: sufixo numérico + índice único como rede de segurança

### Regras implementadas
| Regra | Comportamento |
|-------|---------------|
| Produto em categoria inativa | Bloqueado (422) |
| Variação em produto inativo | Bloqueado (422) |
| Variação duplicada (tamanho+cor) | 409, case-insensitive |
| SKU imutável | `PUT` de variação só altera margem e preço |
| Margem efetiva | Recalculada se preço manual + custo existente |

### Endpoints
| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/products` | 🟢 Todos (paginado, filtros name/categoryId/active) |
| GET | `/products/{id}` | 🟢 Todos (com variações) |
| POST | `/products` | 🟡 Admin + Gestor |
| PUT | `/products/{id}` | 🟡 Admin + Gestor |
| PATCH | `/products/{id}/status` | 🟡 Admin + Gestor |
| POST | `/products/{id}/variants` | 🟡 Admin + Gestor |
| PUT | `/products/{id}/variants/{variantId}` | 🟡 Admin + Gestor |
| PATCH | `/products/{id}/variants/{variantId}/status` | 🟡 Admin + Gestor |

---

## Frontend

### Estrutura
- `types/product.ts`, `services/productService.ts`
- `pages/ProductsPage.tsx` — listagem paginada + filtros (nome, categoria) + modal criar/editar
- `pages/VariantsPanel.tsx` — modal de variações do produto (tabela com SKU + adicionar/ativar/desativar)
- Componente `Pagination` reaproveitado
- Rota `/products` protegida (Admin + Gestor)

### Funcionalidades da tela
- Listagem com variantCount e totalStock por produto
- Criar/editar produto com seleção de categoria
- Painel de variações: mostra o SKU gerado, adiciona variação (tamanho/cor/margem opcional), ativa/desativa
- Ativar/desativar produto com confirmação
- Erros de negócio (duplicata, inativo) exibidos no campo ou toast

---

## Verificação Realizada

### Testes automatizados — 62 testes, 0 falhas
- `SkuGeneratorTest` (6): formato, sequencial, acentos, maiúsculas, colisão com sufixo, categoria curta
- `ProductVariantServiceTest` (7): geração de SKU, bloqueios, conflito, SKU imutável, margem efetiva
- `ProductIntegrationTest` (7): produto+variação com SKU, duplicata 409, categoria/produto inativo 422, sem auth 401, RBAC vendedor 403, **variantCount na listagem**

### Verificação manual (API real)
- SKU gerado corretamente: `BLU-001-M-AZU`, `BLU-002-G-ROS`
- Sequencial, duplicata (409), inativos (422), variantCount na listagem (2) — todos OK
- Telas do frontend compilam no Vite

---

## Notas Técnicas (aprendizados desta feature)

1. **Inconsistência no tech design pega pelos testes:** o exemplo dizia `AZL` para "Azul", mas a regra definida é "3 primeiras letras" = `AZU`. O código estava correto; o teste e o exemplo do design foram ajustados. Reforça o valor de escrever testes.

2. **Bug de LAZY loading pego na verificação manual:** a listagem retornava `variantCount=0` porque a coleção de variações (LAZY) não era carregada fora da transação. Corrigido com `@EntityGraph(attributePaths = ["variants", "category"])` na query de listagem, e coberto por um novo teste de integração para evitar regressão.

---

## Próximos Passos
Com o núcleo (produtos + variações) pronto, o próximo módulo natural é **Estoque** — entrada de mercadoria (lotes), que calcula o custo real com frete rateado e finalmente preenche o `salePrice` das variações pela margem.
