# Modelo de Domínio — EstiloFit Manager

## Diagrama de Entidades

```
┌─────────────┐        ┌──────────────────┐        ┌─────────────────┐
│   Category  │        │     Product      │        │ ProductVariant  │
│─────────────│        │──────────────────│        │─────────────────│
│ id          │◄───────│ id               │◄───────│ id              │
│ name        │  1   N │ name             │  1   N │ sku             │
│ active      │        │ description      │        │ size            │
└─────────────┘        │ category_id (FK) │        │ color           │
                       │ active           │        │ sale_price      │
                       └──────────────────┘        │ average_cost    │
                                                   │ stock_quantity  │
                                                   │ product_id (FK) │
                                                   │ active          │
                                                   └────────┬────────┘
                                                            │
                              ┌─────────────────────────────┼──────────────────────────┐
                              │                             │                          │
                    ┌─────────▼────────┐        ┌──────────▼──────────┐    ┌──────────▼───────┐
                    │ StockMovement    │        │    SupplyLotItem    │    │    SaleItem      │
                    │──────────────────│        │─────────────────────│    │──────────────────│
                    │ id               │        │ id                  │    │ id               │
                    │ variant_id (FK)  │        │ variant_id (FK)     │    │ variant_id (FK)  │
                    │ type             │        │ lot_id (FK)         │    │ sale_id (FK)     │
                    │ quantity         │        │ quantity            │    │ quantity         │
                    │ reference_type   │        │ unit_cost           │    │ unit_price       │
                    │ reference_id     │        │ freight_share       │    └──────────────────┘
                    │ notes            │        │ real_unit_cost      │
                    │ user_id (FK)     │        └──────────┬──────────┘
                    │ created_at       │                   │ N
                    └──────────────────┘                   │
                                                           │ 1
                                                 ┌─────────▼────────┐        ┌─────────────────┐
                                                 │    SupplyLot     │        │    Supplier     │
                                                 │──────────────────│        │─────────────────│
                                                 │ id               │        │ id              │
                                                 │ supplier_id (FK) │───────►│ name            │
                                                 │ received_at      │  N   1 │ contact_phone   │
                                                 │ freight_cost     │        │ contact_email   │
                                                 │ total_cost       │        │ whatsapp        │
                                                 │ notes            │        │ cnpj            │
                                                 └──────────────────┘        │ address         │
                                                                             │ notes           │
                                                                             │ active          │
                                                                             └─────────────────┘


┌──────────────────┐        ┌──────────────────┐        ┌─────────────────┐
│      Sale        │        │   SaleChannel    │        │  PaymentMethod  │
│──────────────────│        │──────────────────│        │─────────────────│
│ id               │        │ id               │        │ (enum)          │
│ channel_id (FK)  │───────►│ name             │        │ CASH            │
│ seller_id (FK)   │  N   1 │ active           │        │ PIX             │
│ confirmed_at     │        └──────────────────┘        │ DEBIT_CARD      │
│ total_amount     │                                     │ CREDIT_CARD     │
│ discount_amount  │        ┌──────────────────┐        └─────────────────┘
│ final_amount     │        │      User        │
│ payment_method   │        │──────────────────│
│ installments     │        │ id               │
│ status           │        │ name             │
│ notes            │        │ email            │
└──────────────────┘        │ password_hash    │
                            │ role             │
                            │ active           │
                            │ created_at       │
                            └──────────────────┘


┌──────────────────┐        ┌──────────────────┐
│ SystemSettings   │        │ PromotionAlert   │
│──────────────────│        │──────────────────│
│ id               │        │ id               │
│ key              │        │ variant_id (FK)  │
│ value            │        │ days_without_sale│
│ description      │        │ suggested_price  │
│ updated_at       │        │ status           │
└──────────────────┘        │ created_at       │
                            │ resolved_at      │
                            └──────────────────┘
```

---

## Detalhamento das Entidades

### Category (Categoria de Produto)

| Coluna    | Tipo         | Restrições        | Descrição                      |
|-----------|--------------|-------------------|--------------------------------|
| id        | UUID         | PK                | Identificador único            |
| name      | VARCHAR(100) | NOT NULL, UNIQUE  | Nome da categoria              |
| active    | BOOLEAN      | NOT NULL, DEFAULT true | Se a categoria está ativa |
| created_at| TIMESTAMP    | NOT NULL          | Data de criação                |

Exemplos: Blusas, Calças, Vestidos, Saias, Shorts, Conjuntos

---

### Product (Produto)

| Coluna      | Tipo         | Restrições        | Descrição                      |
|-------------|--------------|-------------------|--------------------------------|
| id          | UUID         | PK                | Identificador único            |
| name        | VARCHAR(200) | NOT NULL          | Nome do produto                |
| description | TEXT         |                   | Descrição detalhada            |
| category_id | UUID         | FK → Category     | Categoria do produto           |
| active      | BOOLEAN      | NOT NULL, DEFAULT true | Se o produto está ativo   |
| created_at  | TIMESTAMP    | NOT NULL          | Data de criação                |
| updated_at  | TIMESTAMP    | NOT NULL          | Data da última atualização     |

---

### ProductVariant (Variação de Produto)

| Coluna          | Tipo          | Restrições         | Descrição                                |
|-----------------|---------------|--------------------|------------------------------------------|
| id              | UUID          | PK                 | Identificador único                      |
| product_id      | UUID          | FK → Product       | Produto pai                              |
| sku             | VARCHAR(50)   | NOT NULL, UNIQUE   | Código gerado automaticamente            |
| size            | VARCHAR(10)   | NOT NULL           | Tamanho (PP, P, M, G, GG, XGG, etc.)    |
| color           | VARCHAR(50)   | NOT NULL           | Cor da peça                              |
| profit_margin   | DECIMAL(5,2)  |                    | Margem individual (NULL = usa a global)  |
| sale_price      | DECIMAL(10,2) |                    | Preço de venda calculado (custo × margem)|
| average_cost    | DECIMAL(10,2) |                    | Custo médio ponderado (com frete)        |
| stock_quantity  | INTEGER       | NOT NULL, DEFAULT 0| Quantidade atual em estoque              |
| active          | BOOLEAN       | NOT NULL, DEFAULT true | Se a variação está ativa             |
| created_at      | TIMESTAMP     | NOT NULL           | Data de criação                          |
| updated_at      | TIMESTAMP     | NOT NULL           | Data da última atualização               |

**Índice único:** (product_id, size, color) — impede variações duplicadas no mesmo produto.

---

### Supplier (Fornecedor)

| Coluna        | Tipo          | Restrições         | Descrição                       |
|---------------|---------------|--------------------|---------------------------------|
| id            | UUID          | PK                 | Identificador único             |
| name          | VARCHAR(200)  | NOT NULL           | Nome / Razão Social             |
| contact_phone | VARCHAR(20)   |                    | Telefone de contato             |
| contact_email | VARCHAR(200)  |                    | E-mail de contato               |
| whatsapp      | VARCHAR(20)   |                    | WhatsApp                        |
| cnpj          | VARCHAR(18)   | UNIQUE             | CNPJ (opcional)                 |
| address       | TEXT          |                    | Endereço completo               |
| notes         | TEXT          |                    | Observações gerais              |
| active        | BOOLEAN       | NOT NULL, DEFAULT true | Se o fornecedor está ativo  |
| created_at    | TIMESTAMP     | NOT NULL           | Data de criação                 |

---

### SupplyLot (Lote de Entrada de Mercadoria)

| Coluna        | Tipo          | Restrições         | Descrição                             |
|---------------|---------------|--------------------|---------------------------------------|
| id            | UUID          | PK                 | Identificador único                   |
| supplier_id   | UUID          | FK → Supplier      | Fornecedor do lote                    |
| received_at   | DATE          | NOT NULL           | Data de recebimento                   |
| freight_cost  | DECIMAL(10,2) | NOT NULL, DEFAULT 0| Valor do frete da remessa             |
| total_cost    | DECIMAL(10,2) | NOT NULL           | Custo total do lote (mercadoria + frete) |
| notes         | TEXT          |                    | Observações do lote                   |
| created_by    | UUID          | FK → User          | Usuário que registrou                 |
| created_at    | TIMESTAMP     | NOT NULL           | Data de criação do registro           |

---

### SupplyLotItem (Item do Lote de Entrada)

| Coluna          | Tipo          | Restrições          | Descrição                                |
|-----------------|---------------|---------------------|------------------------------------------|
| id              | UUID          | PK                  | Identificador único                      |
| lot_id          | UUID          | FK → SupplyLot      | Lote ao qual pertence                    |
| variant_id      | UUID          | FK → ProductVariant | Variação recebida                        |
| quantity        | INTEGER       | NOT NULL, > 0       | Quantidade recebida                      |
| unit_cost       | DECIMAL(10,2) | NOT NULL            | Custo unitário sem frete                 |
| freight_share   | DECIMAL(10,2) | NOT NULL            | Frete rateado para este item (total)     |
| real_unit_cost  | DECIMAL(10,2) | NOT NULL            | Custo real unitário (com frete rateado)  |

---

### StockMovement (Movimentação de Estoque)

| Coluna         | Tipo         | Restrições          | Descrição                                      |
|----------------|--------------|---------------------|------------------------------------------------|
| id             | UUID         | PK                  | Identificador único                            |
| variant_id     | UUID         | FK → ProductVariant | Variação movimentada                           |
| type           | ENUM         | NOT NULL            | ENTRY, SALE, ADJUSTMENT                        |
| quantity       | INTEGER      | NOT NULL            | Quantidade (positivo = entrada, negativo = saída) |
| reference_type | VARCHAR(50)  |                     | LOT, SALE, MANUAL                              |
| reference_id   | UUID         |                     | ID do lote ou da venda relacionada             |
| notes          | TEXT         |                     | Justificativa (obrigatória para ADJUSTMENT)    |
| user_id        | UUID         | FK → User           | Usuário responsável                            |
| created_at     | TIMESTAMP    | NOT NULL            | Data e hora da movimentação                    |

---

### SaleChannel (Canal de Venda)

| Coluna     | Tipo         | Restrições        | Descrição                      |
|------------|--------------|-------------------|--------------------------------|
| id         | UUID         | PK                | Identificador único            |
| name       | VARCHAR(100) | NOT NULL, UNIQUE  | Nome do canal                  |
| active     | BOOLEAN      | NOT NULL, DEFAULT true | Se o canal está ativo     |
| created_at | TIMESTAMP    | NOT NULL          | Data de criação                |

---

### Sale (Venda)

| Coluna          | Tipo          | Restrições         | Descrição                                  |
|-----------------|---------------|--------------------|--------------------------------------------|
| id              | UUID          | PK                 | Identificador único                        |
| channel_id      | UUID          | FK → SaleChannel   | Canal onde a venda foi realizada           |
| seller_id       | UUID          | FK → User          | Usuário que registrou a venda              |
| confirmed_at    | TIMESTAMP     | NOT NULL           | Data e hora da venda                       |
| total_amount    | DECIMAL(10,2) | NOT NULL           | Valor total dos itens sem desconto         |
| discount_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0| Valor do desconto aplicado                 |
| final_amount    | DECIMAL(10,2) | NOT NULL           | Valor final (total - desconto)             |
| payment_method  | ENUM          | NOT NULL           | CASH, PIX, DEBIT_CARD, CREDIT_CARD         |
| installments    | INTEGER       | NOT NULL, DEFAULT 1| Número de parcelas (1 = à vista)           |
| card_fee_pct    | DECIMAL(5,2)  |                    | Taxa da maquininha em % (só cartão crédito parcelado) |
| card_fee_passed | BOOLEAN       | DEFAULT true       | Se a taxa foi repassada ao cliente         |
| status          | ENUM          | NOT NULL           | CONFIRMED, CANCELLED                       |
| notes           | TEXT          |                    | Observações da venda                       |
| cancelled_by    | UUID          | FK → User          | Usuário que cancelou (se aplicável)        |
| cancelled_at    | TIMESTAMP     |                    | Data do cancelamento (se aplicável)        |
| created_at      | TIMESTAMP     | NOT NULL           | Data de criação do registro                |

---

### SaleInstallment (Parcela de Venda / Conta a Receber)

| Coluna          | Tipo          | Restrições       | Descrição                                       |
|-----------------|---------------|------------------|-------------------------------------------------|
| id              | UUID          | PK               | Identificador único                             |
| sale_id         | UUID          | FK → Sale        | Venda à qual pertence                           |
| installment_num | INTEGER       | NOT NULL         | Número da parcela (1, 2, 3...)                  |
| due_date        | DATE          | NOT NULL         | Data de vencimento (data_venda + N×30 dias)     |
| gross_amount    | DECIMAL(10,2) | NOT NULL         | Valor bruto da parcela                          |
| net_amount      | DECIMAL(10,2) | NOT NULL         | Valor líquido (descontada a taxa da maquininha) |
| status          | ENUM          | NOT NULL         | PENDING, RECEIVED, CANCELLED                    |
| received_at     | TIMESTAMP     |                  | Data em que o recebimento foi confirmado        |
| received_by     | UUID          | FK → User        | Usuário que deu baixa na parcela                |

**Índice único:** (sale_id, installment_num) — impede parcelas duplicadas na mesma venda.

---

### SaleItem (Item da Venda)

| Coluna      | Tipo          | Restrições          | Descrição                              |
|-------------|---------------|---------------------|----------------------------------------|
| id          | UUID          | PK                  | Identificador único                    |
| sale_id     | UUID          | FK → Sale           | Venda à qual pertence                  |
| variant_id  | UUID          | FK → ProductVariant | Variação vendida                       |
| quantity    | INTEGER       | NOT NULL, > 0       | Quantidade vendida                     |
| unit_price  | DECIMAL(10,2) | NOT NULL            | Preço unitário no momento da venda     |
| total_price | DECIMAL(10,2) | NOT NULL            | Subtotal do item (quantity × unit_price)|

---

### User (Usuário)

| Coluna        | Tipo         | Restrições        | Descrição                         |
|---------------|--------------|-------------------|-----------------------------------|
| id            | UUID         | PK                | Identificador único               |
| name          | VARCHAR(200) | NOT NULL          | Nome completo                     |
| email         | VARCHAR(200) | NOT NULL, UNIQUE  | E-mail (usado para login)         |
| password_hash | VARCHAR(255) | NOT NULL          | Hash BCrypt da senha              |
| role          | ENUM         | NOT NULL          | ADMIN, MANAGER, SELLER            |
| active        | BOOLEAN      | NOT NULL, DEFAULT true | Se o usuário pode fazer login |
| created_at    | TIMESTAMP    | NOT NULL          | Data de criação                   |
| updated_at    | TIMESTAMP    | NOT NULL          | Data da última atualização        |

---

### PromotionAlert (Alerta de Promoção)

| Coluna           | Tipo          | Restrições          | Descrição                                  |
|------------------|---------------|---------------------|--------------------------------------------|
| id               | UUID          | PK                  | Identificador único                        |
| variant_id       | UUID          | FK → ProductVariant | Variação com alerta ativo                  |
| days_without_sale| INTEGER       | NOT NULL            | Dias sem venda no momento do alerta        |
| current_price    | DECIMAL(10,2) | NOT NULL            | Preço de venda no momento do alerta        |
| suggested_price  | DECIMAL(10,2) | NOT NULL            | Preço sugerido (50% de desconto)           |
| status           | ENUM          | NOT NULL            | ACTIVE, DISMISSED, RESOLVED               |
| snoozed_until    | DATE          |                     | Data para reexibir (se dispensado)         |
| created_at       | TIMESTAMP     | NOT NULL            | Data de criação do alerta                  |
| resolved_at      | TIMESTAMP     |                     | Data de resolução do alerta               |

---

### SystemSettings (Configurações do Sistema)

| Coluna      | Tipo         | Restrições        | Descrição                              |
|-------------|--------------|-------------------|----------------------------------------|
| id          | UUID         | PK                | Identificador único                    |
| key         | VARCHAR(100) | NOT NULL, UNIQUE  | Chave da configuração                  |
| value       | VARCHAR(500) | NOT NULL          | Valor da configuração                  |
| description | TEXT         |                   | Descrição do que a configuração faz    |
| updated_at  | TIMESTAMP    | NOT NULL          | Data da última alteração               |
| updated_by  | UUID         | FK → User         | Usuário que alterou                    |

**Chaves iniciais:**

| key                          | value | description                                              |
|------------------------------|-------|----------------------------------------------------------|
| DEFAULT_PROFIT_MARGIN        | 100   | Margem de lucro padrão (%) para cálculo do preço de venda |
| PROMOTION_ALERT_DAYS         | 60    | Dias sem venda para gerar alerta de promoção             |
| LOW_STOCK_THRESHOLD          | 2     | Estoque mínimo antes de alertar                          |

---

## Relacionamentos

| De                | Para              | Cardinalidade | Descrição                                  |
|-------------------|-------------------|---------------|--------------------------------------------|
| Product           | Category          | N → 1         | Todo produto pertence a uma categoria      |
| ProductVariant    | Product           | N → 1         | Toda variação pertence a um produto        |
| SupplyLot         | Supplier          | N → 1         | Todo lote vem de um fornecedor             |
| SupplyLotItem     | SupplyLot         | N → 1         | Todo item pertence a um lote               |
| SupplyLotItem     | ProductVariant    | N → 1         | Todo item referencia uma variação          |
| StockMovement     | ProductVariant    | N → 1         | Toda movimentação é de uma variação        |
| StockMovement     | User              | N → 1         | Toda movimentação tem um responsável       |
| Sale              | SaleChannel       | N → 1         | Toda venda tem um canal                    |
| Sale              | User              | N → 1         | Toda venda tem um vendedor                 |
| SaleItem          | Sale              | N → 1         | Todo item pertence a uma venda             |
| SaleItem          | ProductVariant    | N → 1         | Todo item referencia uma variação          |
| SaleInstallment   | Sale              | N → 1         | Toda parcela pertence a uma venda          |
| SaleInstallment   | User              | N → 1         | Toda baixa tem um responsável              |
| PromotionAlert    | ProductVariant    | N → 1         | Todo alerta é de uma variação              |

---

## Enumerações

### Role (Perfil de Usuário)
```
ADMIN    → Administrador
MANAGER  → Gestor
SELLER   → Vendedor
```

### PaymentMethod (Forma de Pagamento)
```
CASH         → Dinheiro
PIX          → PIX
DEBIT_CARD   → Cartão de Débito
CREDIT_CARD  → Cartão de Crédito
```

### SaleStatus (Status da Venda)
```
CONFIRMED  → Venda confirmada e ativa
CANCELLED  → Venda cancelada
```

### StockMovementType (Tipo de Movimentação)
```
ENTRY       → Entrada de mercadoria (lote)
SALE        → Saída por venda
ADJUSTMENT  → Ajuste manual
```

### InstallmentStatus (Status da Parcela)
```
PENDING    → Aguardando recebimento
RECEIVED   → Recebimento confirmado
CANCELLED  → Cancelada junto com a venda
```

### PromotionAlertStatus (Status do Alerta de Promoção)
```
ACTIVE    → Alerta ativo, exibido no dashboard
DISMISSED → Dispensado temporariamente (snoozed)
RESOLVED  → Resolvido (venda realizada ou dismissal permanente)
```
