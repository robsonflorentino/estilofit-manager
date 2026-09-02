# Tech Design — Feature 003: Produtos + Variações

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/api-products-variants`        |
| Status        | ✅ Design aprovado (2026-09-01)         |
| Depende de    | Feature 001 (auth), Feature 002 (categorias) |
| Regras        | RN-001 a RN-006                        |

> Este é um documento de **design prévio** — decisões a validar **antes** de escrever código.

---

## 1. Objetivo

Implementar o cadastro de produtos e suas variações (tamanho + cor), com **geração automática de SKU**. É o núcleo do sistema: estoque, vendas e relatórios dependem das variações.

---

## 2. Modelo (já existe no banco — migration V3)

```
products                          product_variants
├── id (UUID)                     ├── id (UUID)
├── name                          ├── product_id (FK)
├── description                   ├── sku (único)
├── category_id (FK)              ├── size
├── active                        ├── color
├── created_at / updated_at       ├── profit_margin  (nullable → herda global)
                                  ├── sale_price     (nullable até ter custo)
                                  ├── average_cost   (nullable até 1ª entrada)
                                  ├── stock_quantity (default 0)
                                  ├── active
                                  └── created_at / updated_at

Índice único composto: (product_id, size, color)
```

Nenhuma migration nova é necessária. O design foca na lógica de aplicação.

---

## 3. Decisões a Validar

### 3.1. Formato e geração do SKU (RN-003) — **ponto central**

Formato definido: `{PREFIXO}-{SEQUENCIAL}-{TAMANHO}-{COR}` → ex: `BLU-001-M-AZU`

Proponho as seguintes regras de derivação de cada parte:

| Parte        | Regra proposta                                                        | Exemplo |
|--------------|-----------------------------------------------------------------------|---------|
| `PREFIXO`    | 3 primeiras letras do **nome do produto**, maiúsculas, sem acento     | "Blusa Listrada" → `BLS` |
| `SEQUENCIAL` | Contador por prefixo, 3 dígitos com zero à esquerda                    | `001`, `002`... |
| `TAMANHO`    | O tamanho como informado, maiúsculo                                   | `M`, `GG` |
| `COR`        | 3 primeiras letras da cor, maiúsculas, sem acento                     | "Azul" → `AZU` |

**Pontos que preciso da sua decisão:**

**(a) Prefixo baseado no produto ou na categoria?**
- **Opção A — nome do produto** (`BLS` de "Blusa Listrada"): mais específico, mas dois produtos "Blusa X" e "Blusa Y" teriam o mesmo prefixo `BLS`.
- **Opção B — categoria** (`BLU` de "Blusas"): agrupa por categoria, mais previsível.
- **Minha recomendação: Opção B (categoria)** — é mais estável e o sequencial garante unicidade dentro do grupo.

**(b) O sequencial é por prefixo ou global?**
- Proponho **por prefixo**: `BLU-001`, `BLU-002`, `CAL-001`... Cada categoria tem sua própria contagem. Mais legível.

**(c) Colisão de cor no prefixo de 3 letras**
- "Azul" e "Azeviche" viram ambos `AZ?`. Com 3 letras: `AZU` vs `AZE` — ok, diferenciam. Mas "Rosa" e "Roxo" viram `ROS` e `ROX` — ok também. O risco existe mas é baixo. Proponho **aceitar 3 letras** e, se houver colisão real de SKU, o índice único do banco barra e o sistema tenta um sufixo. Aceita?

**(d) Concorrência no sequencial**
- Dois cadastros simultâneos poderiam gerar o mesmo sequencial. Proponho resolver com **uma tabela/consulta que pega o MAX(sequencial) do prefixo dentro de uma transação**, e o índice único como rede de segurança (se colidir, tenta o próximo). Para o volume da loja isso é mais que suficiente — não precisa de sequence dedicada no Postgres.

---

### 3.2. Preço de venda e margem (RN-005, RN-006)

No cadastro da variação, **não há custo ainda** (o custo vem da 1ª entrada de mercadoria, feature futura). Então:

- Ao criar a variação: `average_cost = null`, `sale_price = null`, `profit_margin = null` (herda global) ou o valor informado
- O `sale_price` só é calculável quando existir `average_cost`
- **Nesta feature**, a variação nasce sem preço. O cálculo automático `custo × (1 + margem/100)` será acionado na feature de **entrada de mercadoria**

**Decisão a validar:** nesta feature, o `PUT /variants/{id}` permite:
- Alterar `profitMargin` (afeta cálculo futuro)
- Informar `salePrice` manualmente (RN-006 permite override manual)

Quando ambos existirem (`salePrice` manual + `averageCost`), o sistema calcula e exibe a **margem efetiva** — mas isso é mais relevante quando houver custo. Nesta feature, guardamos os campos; a exibição da margem efetiva fica trivial. Concorda?

---

### 3.3. Endpoints (já no contrato da API)

| Método | Rota                                            | Permissão         |
|--------|-------------------------------------------------|-------------------|
| GET    | `/products`                                     | 🟢 Todos          |
| POST   | `/products`                                     | 🟡 Admin + Gestor |
| GET    | `/products/{id}`                                | 🟢 Todos          |
| PUT    | `/products/{id}`                                | 🟡 Admin + Gestor |
| PATCH  | `/products/{id}/status`                         | 🟡 Admin + Gestor |
| POST   | `/products/{id}/variants`                       | 🟡 Admin + Gestor |
| PUT    | `/products/{id}/variants/{variantId}`           | 🟡 Admin + Gestor |
| PATCH  | `/products/{id}/variants/{variantId}/status`    | 🟡 Admin + Gestor |

- `GET /products` é **paginado** com filtros (`name`, `categoryId`, `active`) — segue ADR-010
- `GET /products/{id}` retorna o produto **com suas variações**
- Listagem traz campos derivados: `variantCount` e `totalStock` (soma do estoque das variações)

---

### 3.4. Regras de validação e casos de borda

| Situação | Comportamento proposto |
|----------|------------------------|
| Criar produto com categoria inexistente | 404 |
| Criar produto em categoria inativa | **Decisão:** permitir ou bloquear (422)? Proponho **bloquear** |
| Criar variação duplicada (mesmo size+color no produto) | 409 |
| Criar variação em produto inativo | Proponho **bloquear** (422) |
| SKU é imutável (RN-003) | `PUT` de variação **não** altera size/color/sku — só margin e salePrice |
| Desativar produto | Não desativa as variações em cascata (elas seguem consultáveis) — **validar se concorda** |

> Destaque no `PUT /variants`: como o SKU deriva de size+color e é imutável, **não permitimos alterar size/color** depois de criada. Se precisar mudar, cria-se outra variação. Isso está alinhado com a RN-003. Confirma?

---

## 4. Estrutura de Código (padrão do projeto)

```
product/
├── domain/
│   ├── Product.kt
│   └── ProductVariant.kt
├── repository/
│   ├── ProductRepository.kt
│   └── ProductVariantRepository.kt
├── dto/
│   ├── ProductDTOs.kt
│   └── VariantDTOs.kt
├── service/
│   ├── ProductService.kt
│   ├── ProductVariantService.kt
│   └── SkuGenerator.kt        ← lógica isolada de geração de SKU (testável)
└── controller/
    ├── ProductController.kt
    └── ProductVariantController.kt
```

O `SkuGenerator` fica isolado justamente para concentrar a lógica não-trivial de SKU e facilitar testes unitários.

---

## 5. Decisões Aprovadas (2026-09-01)

Todas seguindo as recomendações:

1. **Prefixo do SKU:** derivado da **categoria** (3 primeiras letras, maiúsculas, sem acento). Ex: "Blusas" → `BLU`
2. **Sequencial:** **por prefixo**, 3 dígitos com zero à esquerda. Ex: `BLU-001`, `BLU-002`, `CAL-001`
3. **Colisão de cor:** aceitar 3 letras da cor; se o SKU final colidir, adicionar sufixo numérico. Índice único do banco como rede de segurança
4. **Cadastro em item inativo:** **bloquear** (422) — não criar variação em produto inativo, nem produto em categoria inativa
5. **SKU imutável:** confirmado — `PUT` de variação altera apenas `profitMargin` e `salePrice`, nunca `size`/`color` (que compõem o SKU)

**Formato final do SKU:** `{PREFIXO_CATEGORIA}-{SEQUENCIAL}-{TAMANHO}-{COR}` → ex: `BLU-001-M-AZU` (categoria "Blusas", tamanho M, cor "Azul")
