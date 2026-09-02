# Contrato da API — EstiloFit Manager

Base URL: `http://localhost:8080/api/v1`

Todos os endpoints (exceto `/auth/login` e `/auth/refresh`) exigem o header:
```
Authorization: Bearer <accessToken>
```

Legenda de permissões: 🔴 Admin | 🟡 Admin + Gestor | 🟢 Admin + Gestor + Vendedor

---

## Sumário
1. [Autenticação](#1-autenticação)
2. [Usuários](#2-usuários)
3. [Produtos](#3-produtos)
4. [Variações de Produto](#4-variações-de-produto)
5. [Categorias](#5-categorias)
6. [Fornecedores](#6-fornecedores)
7. [Lotes de Entrada (Estoque)](#7-lotes-de-entrada-estoque)
8. [Movimentações de Estoque](#8-movimentações-de-estoque)
9. [Canais de Venda](#9-canais-de-venda)
10. [Vendas](#10-vendas)
11. [Parcelas (Contas a Receber)](#11-parcelas-contas-a-receber)
12. [Alertas de Promoção](#12-alertas-de-promoção)
13. [Relatórios](#13-relatórios)
14. [Configurações do Sistema](#14-configurações-do-sistema)

---

## 1. Autenticação

### POST `/auth/login` — pública
Autentica o usuário e retorna os tokens.

**Request:**
```json
{
  "email": "gestor@estilofit.com.br",
  "password": "minhasenha123"
}
```
**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "user": {
    "id": "uuid",
    "name": "Ana Paula",
    "email": "gestor@estilofit.com.br",
    "role": "MANAGER"
  }
}
```
**Errors:** `401` credenciais inválidas | `401` usuário desativado

---

### POST `/auth/refresh` — pública (via httpOnly cookie)
Renova o access token usando o refresh token.

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 28800
}
```
**Errors:** `401` refresh token expirado ou inválido

---

### POST `/auth/logout` — 🟢 qualquer role
Invalida o refresh token (apaga o httpOnly cookie).

**Response:** `204 No Content`

---

## 2. Usuários

### GET `/users` — 🔴 Admin
Lista todos os usuários.

**Query params:** `page`, `size`, `name`, `role`, `active`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Ana Paula",
      "email": "ana@estilofit.com.br",
      "role": "MANAGER",
      "active": true,
      "createdAt": "2026-09-01T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### POST `/users` — 🔴 Admin
Cria um novo usuário.

**Request:**
```json
{
  "name": "Carlos Vendedor",
  "email": "carlos@estilofit.com.br",
  "password": "senha12345",
  "role": "SELLER"
}
```
**Response 201:**
```json
{
  "id": "uuid",
  "name": "Carlos Vendedor",
  "email": "carlos@estilofit.com.br",
  "role": "SELLER",
  "active": true,
  "createdAt": "2026-09-01T10:00:00"
}
```
**Errors:** `409` email já cadastrado | `400` dados inválidos

---

### GET `/users/{id}` — 🔴 Admin
Retorna os dados de um usuário específico.

**Response 200:** mesmo schema do item acima
**Errors:** `404` usuário não encontrado

---

### PUT `/users/{id}` — 🔴 Admin
Atualiza dados do usuário (não altera senha).

**Request:**
```json
{
  "name": "Carlos Silva",
  "email": "carlos.silva@estilofit.com.br",
  "role": "SELLER"
}
```
**Response 200:** usuário atualizado
**Errors:** `404` | `409` email em uso

---

### PATCH `/users/{id}/status` — 🔴 Admin
Ativa ou desativa um usuário.

**Request:**
```json
{ "active": false }
```
**Response 200:** usuário com status atualizado

---

### PATCH `/users/{id}/password` — 🔴 Admin
Redefine a senha de um usuário.

**Request:**
```json
{ "newPassword": "novaSenha123" }
```
**Response:** `204 No Content`

---

### GET `/users/me` — 🟢 qualquer role
Retorna os dados do usuário logado.

**Response 200:** dados do usuário logado

---

### PATCH `/users/me/password` — 🟢 qualquer role
Permite que o usuário logado altere sua própria senha.

**Request:**
```json
{
  "currentPassword": "senhaAtual",
  "newPassword": "novaSenha123"
}
```
**Response:** `204 No Content`
**Errors:** `401` senha atual incorreta

---

## 3. Produtos

### GET `/products` — 🟢 qualquer role
Lista produtos com filtros e paginação.

**Query params:** `page`, `size`, `name`, `categoryId`, `active`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Blusa Listrada",
      "description": "Blusa fitness listrada em poliamida",
      "category": { "id": "uuid", "name": "Blusas" },
      "active": true,
      "variantCount": 3,
      "totalStock": 14,
      "createdAt": "2026-09-01T10:00:00"
    }
  ],
  "page": 0, "size": 20, "totalElements": 42, "totalPages": 3
}
```

---

### POST `/products` — 🟡 Admin + Gestor
Cria um novo produto.

**Request:**
```json
{
  "name": "Blusa Listrada",
  "description": "Blusa fitness listrada em poliamida",
  "categoryId": "uuid"
}
```
**Response 201:** produto criado com id
**Errors:** `400` dados inválidos | `404` categoria não encontrada

---

### GET `/products/{id}` — 🟢 qualquer role
Retorna detalhes do produto com suas variações.

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Blusa Listrada",
  "description": "...",
  "category": { "id": "uuid", "name": "Blusas" },
  "active": true,
  "variants": [
    {
      "id": "uuid",
      "sku": "BLS-001-M-AZL",
      "size": "M",
      "color": "Azul",
      "salePrice": 89.90,
      "averageCost": 40.00,
      "profitMargin": 100.0,
      "stockQuantity": 5,
      "active": true
    }
  ]
}
```

---

### PUT `/products/{id}` — 🟡 Admin + Gestor
Atualiza nome, descrição ou categoria do produto.

**Request:**
```json
{
  "name": "Blusa Listrada Premium",
  "description": "Nova descrição",
  "categoryId": "uuid"
}
```
**Response 200:** produto atualizado
**Errors:** `404` produto não encontrado

---

### PATCH `/products/{id}/status` — 🟡 Admin + Gestor
Ativa ou desativa um produto.

**Request:** `{ "active": false }`
**Response 200:** produto atualizado

---

## 4. Variações de Produto

### POST `/products/{productId}/variants` — 🟡 Admin + Gestor
Adiciona uma variação ao produto.

**Request:**
```json
{
  "size": "M",
  "color": "Azul",
  "profitMargin": null
}
```
> `profitMargin: null` herda a margem global configurada no sistema.

**Response 201:**
```json
{
  "id": "uuid",
  "sku": "BLS-001-M-AZL",
  "size": "M",
  "color": "Azul",
  "salePrice": null,
  "averageCost": null,
  "profitMargin": null,
  "stockQuantity": 0,
  "active": true
}
```
**Errors:** `409` variação com mesmo tamanho/cor já existe neste produto

---

### PUT `/products/{productId}/variants/{variantId}` — 🟡 Admin + Gestor
Atualiza margem ou preço de venda de uma variação.

**Request:**
```json
{
  "profitMargin": 120.0,
  "salePrice": 99.90
}
```
> Se `salePrice` for informado manualmente, o sistema registra mas exibe a margem efetiva resultante.

**Response 200:** variação atualizada

---

### PATCH `/products/{productId}/variants/{variantId}/status` — 🟡 Admin + Gestor
Ativa ou desativa uma variação.

**Request:** `{ "active": false }`
**Response 200:** variação atualizada

---

## 5. Categorias

### GET `/categories` — 🟢 qualquer role
Lista todas as categorias ativas.

**Response 200:**
```json
[
  { "id": "uuid", "name": "Blusas", "active": true },
  { "id": "uuid", "name": "Calças", "active": true }
]
```

---

### POST `/categories` — 🟡 Admin + Gestor
Cria uma nova categoria.

**Request:** `{ "name": "Conjuntos" }`
**Response 201:** categoria criada
**Errors:** `409` nome já existe

---

### PUT `/categories/{id}` — 🟡 Admin + Gestor
Renomeia uma categoria.

**Request:** `{ "name": "Conjuntos Fitness" }`
**Response 200:** categoria atualizada

---

### PATCH `/categories/{id}/status` — 🟡 Admin + Gestor
Ativa ou desativa uma categoria.

**Request:** `{ "active": false }`
**Response 200:** categoria atualizada

---

## 6. Fornecedores

### GET `/suppliers` — 🟡 Admin + Gestor
Lista fornecedores com paginação e filtros.

**Query params:** `page`, `size`, `name`, `active`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Moda Brasil LTDA",
      "contactPhone": "(11) 99999-0000",
      "contactEmail": "contato@modabrasil.com.br",
      "whatsapp": "(11) 99999-0000",
      "cnpj": "12.345.678/0001-99",
      "active": true
    }
  ],
  "page": 0, "size": 20, "totalElements": 5, "totalPages": 1
}
```

---

### POST `/suppliers` — 🟡 Admin + Gestor
Cadastra um novo fornecedor.

**Request:**
```json
{
  "name": "Moda Brasil LTDA",
  "contactPhone": "(11) 99999-0000",
  "contactEmail": "contato@modabrasil.com.br",
  "whatsapp": "(11) 99999-0000",
  "cnpj": "12.345.678/0001-99",
  "address": "Rua das Flores, 100 - São Paulo/SP",
  "notes": "Pagamento em 30 dias"
}
```
**Response 201:** fornecedor criado
**Errors:** `409` CNPJ já cadastrado

---

### GET `/suppliers/{id}` — 🟡 Admin + Gestor
Retorna detalhes de um fornecedor.

**Response 200:** dados completos do fornecedor

---

### PUT `/suppliers/{id}` — 🟡 Admin + Gestor
Atualiza dados do fornecedor.

**Response 200:** fornecedor atualizado

---

### PATCH `/suppliers/{id}/status` — 🟡 Admin + Gestor
Ativa ou desativa um fornecedor.

**Request:** `{ "active": false }`
**Response 200:** fornecedor atualizado

---

## 7. Lotes de Entrada (Estoque)

### GET `/supply-lots` — 🟡 Admin + Gestor
Lista lotes de entrada com paginação.

**Query params:** `page`, `size`, `supplierId`, `startDate`, `endDate`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "supplier": { "id": "uuid", "name": "Moda Brasil LTDA" },
      "receivedAt": "2026-09-01",
      "freightCost": 200.00,
      "totalCost": 1200.00,
      "itemCount": 5,
      "createdBy": { "id": "uuid", "name": "Ana Paula" },
      "createdAt": "2026-09-01T10:00:00"
    }
  ],
  "page": 0, "size": 20, "totalElements": 12, "totalPages": 1
}
```

---

### POST `/supply-lots` — 🟡 Admin + Gestor
Registra um novo lote de entrada. Ao confirmar, o estoque é atualizado e o custo médio recalculado automaticamente.

**Request:**
```json
{
  "supplierId": "uuid",
  "receivedAt": "2026-09-01",
  "freightCost": 200.00,
  "notes": "Pedido #1234",
  "items": [
    {
      "variantId": "uuid",
      "quantity": 10,
      "unitCost": 30.00
    },
    {
      "variantId": "uuid",
      "quantity": 5,
      "unitCost": 60.00
    }
  ]
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "supplier": { "id": "uuid", "name": "Moda Brasil LTDA" },
  "receivedAt": "2026-09-01",
  "freightCost": 200.00,
  "totalCost": 1200.00,
  "notes": "Pedido #1234",
  "items": [
    {
      "id": "uuid",
      "variant": { "id": "uuid", "sku": "BLS-001-M-AZL", "size": "M", "color": "Azul" },
      "quantity": 10,
      "unitCost": 30.00,
      "freightShare": 100.00,
      "realUnitCost": 40.00
    }
  ]
}
```
**Errors:** `404` fornecedor ou variação não encontrada | `400` itens vazios

---

### GET `/supply-lots/{id}` — 🟡 Admin + Gestor
Retorna detalhes completos de um lote.

**Response 200:** dados completos com itens e rateio de frete

---

## 8. Movimentações de Estoque

### GET `/stock/movements` — 🟡 Admin + Gestor
Lista o histórico de movimentações com filtros.

**Query params:** `page`, `size`, `variantId`, `type`, `startDate`, `endDate`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "variant": { "id": "uuid", "sku": "BLS-001-M-AZL", "productName": "Blusa Listrada" },
      "type": "ENTRY",
      "quantity": 10,
      "referenceType": "LOT",
      "referenceId": "uuid",
      "notes": null,
      "user": { "id": "uuid", "name": "Ana Paula" },
      "createdAt": "2026-09-01T10:00:00"
    }
  ],
  "page": 0, "size": 20, "totalElements": 50, "totalPages": 3
}
```

---

### GET `/stock/summary` — 🟢 qualquer role
Retorna o resumo de estoque atual por variação.

**Query params:** `page`, `size`, `productId`, `categoryId`, `size` (tamanho), `color`, `lowStock`

**Response 200:**
```json
{
  "content": [
    {
      "variantId": "uuid",
      "sku": "BLS-001-M-AZL",
      "productName": "Blusa Listrada",
      "category": "Blusas",
      "size": "M",
      "color": "Azul",
      "stockQuantity": 5,
      "salePrice": 89.90,
      "averageCost": 40.00,
      "isLowStock": false
    }
  ],
  "page": 0, "size": 20, "totalElements": 30, "totalPages": 2
}
```

---

### POST `/stock/adjustments` — 🟡 Admin + Gestor
Registra um ajuste manual de estoque.

**Request:**
```json
{
  "variantId": "uuid",
  "quantity": -2,
  "notes": "Peças danificadas encontradas no inventário"
}
```
> `quantity` positivo = entrada, negativo = saída.

**Response 201:** movimentação registrada
**Errors:** `400` estoque insuficiente para saída | `400` justificativa obrigatória

---

## 9. Canais de Venda

### GET `/sale-channels` — 🟢 qualquer role
Lista canais de venda ativos.

**Response 200:**
```json
[
  { "id": "uuid", "name": "Instagram", "active": true },
  { "id": "uuid", "name": "WhatsApp", "active": true },
  { "id": "uuid", "name": "Presencial", "active": true }
]
```

---

### POST `/sale-channels` — 🟡 Admin + Gestor
Cria um novo canal de venda.

**Request:** `{ "name": "Shopee" }`
**Response 201:** canal criado

---

### PATCH `/sale-channels/{id}/status` — 🟡 Admin + Gestor
Ativa ou desativa um canal.

**Request:** `{ "active": false }`
**Response 200:** canal atualizado

---

## 10. Vendas

### GET `/sales` — 🟡 Admin + Gestor (loja toda) | 🟢 Vendedor (próprias)
Lista vendas. Vendedor recebe automaticamente apenas suas próprias vendas.

**Query params:** `page`, `size`, `channelId`, `paymentMethod`, `status`, `startDate`, `endDate`, `sellerId`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "channel": { "id": "uuid", "name": "Instagram" },
      "seller": { "id": "uuid", "name": "Carlos" },
      "confirmedAt": "2026-09-01T14:30:00",
      "totalAmount": 179.80,
      "discountAmount": 0.00,
      "finalAmount": 179.80,
      "paymentMethod": "PIX",
      "installments": 1,
      "status": "CONFIRMED",
      "itemCount": 2
    }
  ],
  "page": 0, "size": 20, "totalElements": 85, "totalPages": 5
}
```

---

### POST `/sales` — 🟢 qualquer role
Registra e confirma uma nova venda. Operação atômica: valida estoque, debita e salva.

**Request:**
```json
{
  "channelId": "uuid",
  "paymentMethod": "CREDIT_CARD",
  "installments": 3,
  "cardFeePct": 3.5,
  "cardFeePassed": true,
  "discountAmount": 0.00,
  "notes": "Cliente: Maria Silva",
  "items": [
    { "variantId": "uuid", "quantity": 1, "unitPrice": 89.90 },
    { "variantId": "uuid", "quantity": 1, "unitPrice": 89.90 }
  ]
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "channel": { "id": "uuid", "name": "Instagram" },
  "seller": { "id": "uuid", "name": "Carlos" },
  "confirmedAt": "2026-09-01T14:30:00",
  "totalAmount": 179.80,
  "discountAmount": 0.00,
  "finalAmount": 179.80,
  "paymentMethod": "CREDIT_CARD",
  "installments": 3,
  "cardFeePct": 3.5,
  "cardFeePassed": true,
  "status": "CONFIRMED",
  "items": [
    {
      "id": "uuid",
      "variant": { "id": "uuid", "sku": "BLS-001-M-AZL", "productName": "Blusa Listrada", "size": "M", "color": "Azul" },
      "quantity": 1,
      "unitPrice": 89.90,
      "totalPrice": 89.90
    }
  ],
  "installmentSchedule": [
    { "installmentNum": 1, "dueDate": "2026-10-01", "grossAmount": 59.93, "netAmount": 57.83, "status": "PENDING" },
    { "installmentNum": 2, "dueDate": "2026-11-01", "grossAmount": 59.93, "netAmount": 57.83, "status": "PENDING" },
    { "installmentNum": 3, "dueDate": "2026-12-01", "grossAmount": 59.94, "netAmount": 57.84, "status": "PENDING" }
  ]
}
```
**Errors:** `422` estoque insuficiente para uma ou mais variações | `400` dados inválidos | `404` canal ou variação não encontrada

---

### GET `/sales/{id}` — 🟡 Admin + Gestor | 🟢 Vendedor (próprias)
Retorna detalhes completos de uma venda.

**Response 200:** dados completos com itens e parcelas (se parcelado)
**Errors:** `404` | `403` vendedor tentando acessar venda de outro

---

### PATCH `/sales/{id}/cancel` — 🟡 Admin + Gestor
Cancela uma venda confirmada. Restaura estoque automaticamente.

**Request:** `{ "reason": "Cliente desistiu da compra" }`
**Response 200:** venda com status `CANCELLED`
**Errors:** `404` | `422` venda já cancelada

---

## 11. Parcelas (Contas a Receber)

### GET `/installments` — 🟡 Admin + Gestor
Lista parcelas com filtros.

**Query params:** `page`, `size`, `status`, `startDueDate`, `endDueDate`, `saleId`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "sale": { "id": "uuid", "confirmedAt": "2026-09-01T14:30:00", "finalAmount": 179.80 },
      "installmentNum": 1,
      "dueDate": "2026-10-01",
      "grossAmount": 59.93,
      "netAmount": 57.83,
      "status": "PENDING",
      "receivedAt": null
    }
  ],
  "page": 0, "size": 20, "totalElements": 9, "totalPages": 1
}
```

---

### PATCH `/installments/{id}/receive` — 🟡 Admin + Gestor
Confirma o recebimento de uma parcela.

**Request:** `{ "receivedAt": "2026-10-02" }`
**Response 200:** parcela com status `RECEIVED`
**Errors:** `404` | `422` parcela já recebida ou cancelada

---

### GET `/installments/projected` — 🟡 Admin + Gestor
Retorna o fluxo de caixa projetado agrupado por mês.

**Query params:** `months` (padrão: 3)

**Response 200:**
```json
[
  {
    "month": "2026-10",
    "totalGross": 450.00,
    "totalNet": 434.25,
    "installments": [
      {
        "id": "uuid",
        "saleId": "uuid",
        "installmentNum": 1,
        "dueDate": "2026-10-01",
        "grossAmount": 59.93,
        "netAmount": 57.83,
        "status": "PENDING"
      }
    ]
  }
]
```

---

## 12. Alertas de Promoção

### GET `/promotion-alerts` — 🟡 Admin + Gestor
Lista alertas de promoção ativos.

**Query params:** `page`, `size`, `status`

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "variant": {
        "id": "uuid",
        "sku": "BLS-001-M-AZL",
        "productName": "Blusa Listrada",
        "size": "M",
        "color": "Azul",
        "stockQuantity": 3
      },
      "daysWithoutSale": 75,
      "currentPrice": 89.90,
      "suggestedPrice": 44.95,
      "status": "ACTIVE",
      "createdAt": "2026-09-01T08:00:00"
    }
  ],
  "page": 0, "size": 20, "totalElements": 4, "totalPages": 1
}
```

---

### PATCH `/promotion-alerts/{id}/dismiss` — 🟡 Admin + Gestor
Descarta um alerta temporariamente (snooze).

**Request:**
```json
{ "snoozedUntil": "2026-10-01" }
```
**Response 200:** alerta com status `DISMISSED`

---

### PATCH `/promotion-alerts/{id}/resolve` — 🟡 Admin + Gestor
Resolve permanentemente um alerta (sem aplicar desconto).

**Response 200:** alerta com status `RESOLVED`

---

## 13. Relatórios

### GET `/reports/stock` — 🟡 Admin + Gestor
Relatório de estoque atual com alertas de baixo estoque.

**Query params:** `categoryId`, `lowStockOnly`

**Response 200:**
```json
{
  "generatedAt": "2026-09-01T10:00:00",
  "totalVariants": 42,
  "totalItems": 187,
  "lowStockCount": 5,
  "zeroStockCount": 2,
  "items": [
    {
      "sku": "BLS-001-M-AZL",
      "productName": "Blusa Listrada",
      "category": "Blusas",
      "size": "M",
      "color": "Azul",
      "stockQuantity": 1,
      "salePrice": 89.90,
      "averageCost": 40.00,
      "isLowStock": true,
      "isZeroStock": false
    }
  ]
}
```

---

### GET `/reports/sales` — 🟡 Admin + Gestor
Relatório de vendas do período com agrupamentos.

**Query params:** `startDate`, `endDate`, `channelId`, `paymentMethod`

**Response 200:**
```json
{
  "period": { "start": "2026-09-01", "end": "2026-09-30" },
  "totalSales": 45,
  "totalRevenue": 4320.50,
  "totalDiscount": 120.00,
  "netRevenue": 4200.50,
  "byChannel": [
    { "channel": "Instagram", "salesCount": 20, "revenue": 1800.00 }
  ],
  "byPaymentMethod": [
    { "method": "PIX", "salesCount": 18, "revenue": 1620.00 }
  ]
}
```

---

### GET `/reports/top-products` — 🟡 Admin + Gestor
Ranking de produtos mais vendidos no período.

**Query params:** `startDate`, `endDate`, `limit` (padrão: 10)

**Response 200:**
```json
{
  "period": { "start": "2026-09-01", "end": "2026-09-30" },
  "items": [
    {
      "rank": 1,
      "productName": "Blusa Listrada",
      "category": "Blusas",
      "quantitySold": 18,
      "totalRevenue": 1618.20,
      "topVariant": { "sku": "BLS-001-M-AZL", "size": "M", "color": "Azul", "quantitySold": 10 }
    }
  ]
}
```

---

### GET `/reports/profit-margin` — 🟡 Admin + Gestor
Margem de lucro por produto/variação no período.

**Query params:** `startDate`, `endDate`, `categoryId`

**Response 200:**
```json
{
  "period": { "start": "2026-09-01", "end": "2026-09-30" },
  "items": [
    {
      "sku": "BLS-001-M-AZL",
      "productName": "Blusa Listrada",
      "size": "M",
      "color": "Azul",
      "quantitySold": 10,
      "averageCost": 40.00,
      "averageSalePrice": 89.90,
      "totalRevenue": 899.00,
      "totalCost": 400.00,
      "grossProfit": 499.00,
      "marginPct": 55.51
    }
  ]
}
```

---

### GET `/reports/pro-labore` — 🟡 Admin + Gestor
Estimativa de pró-labore do mês atual.

**Query params:** `month` (formato: `2026-09`, padrão: mês atual)

**Response 200:**
```json
{
  "month": "2026-09",
  "revenue": 4200.50,
  "cogs": 1890.00,
  "freightCosts": 400.00,
  "grossProfit": 1910.50,
  "workingCapitalEstimate": 1300.00,
  "workingCapitalBasedOnMonths": 3,
  "suggestedProLabore": 610.50,
  "canWithdraw": true,
  "warning": null
}
```
> Quando `canWithdraw: false`, o campo `warning` contém a mensagem explicativa.

---

### GET `/reports/my-sales` — 🟢 qualquer role
Relatório de vendas do usuário logado no período.

**Query params:** `startDate`, `endDate`

**Response 200:**
```json
{
  "period": { "start": "2026-09-01", "end": "2026-09-30" },
  "sellerId": "uuid",
  "sellerName": "Carlos",
  "totalSales": 12,
  "totalRevenue": 1080.00,
  "sales": [
    {
      "id": "uuid",
      "confirmedAt": "2026-09-05T14:30:00",
      "channel": "Instagram",
      "finalAmount": 89.90,
      "paymentMethod": "PIX",
      "status": "CONFIRMED"
    }
  ]
}
```

---

## 14. Configurações do Sistema

### GET `/settings` — 🔴 Admin
Lista todas as configurações do sistema.

**Response 200:**
```json
[
  { "key": "DEFAULT_PROFIT_MARGIN", "value": "100", "description": "Margem de lucro padrão (%)" },
  { "key": "PROMOTION_ALERT_DAYS", "value": "60", "description": "Dias sem venda para alerta de promoção" },
  { "key": "LOW_STOCK_THRESHOLD", "value": "2", "description": "Estoque mínimo para alerta" }
]
```

---

### PUT `/settings/{key}` — 🔴 Admin
Atualiza o valor de uma configuração.

**Request:** `{ "value": "90" }`
**Response 200:** configuração atualizada
**Errors:** `404` chave não encontrada | `400` valor inválido para o tipo da configuração

---

## Formato Padrão de Erros

Todos os erros seguem o mesmo formato:

```json
{
  "timestamp": "2026-09-01T10:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Estoque insuficiente para a variação BLS-001-M-AZL. Disponível: 2, solicitado: 5.",
  "path": "/api/v1/sales"
}
```

Erros de validação de campos:
```json
{
  "timestamp": "2026-09-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "fieldErrors": [
    { "field": "name", "message": "Nome é obrigatório" },
    { "field": "categoryId", "message": "Categoria é obrigatória" }
  ]
}
```
