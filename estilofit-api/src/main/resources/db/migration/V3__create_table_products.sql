-- V3: Produtos e variações
CREATE TABLE products (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category_id UUID         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active      ON products (active);
CREATE INDEX idx_products_name        ON products USING gin (to_tsvector('portuguese', name));

CREATE TABLE product_variants (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    product_id     UUID           NOT NULL,
    sku            VARCHAR(50)    NOT NULL,
    size           VARCHAR(10)    NOT NULL,
    color          VARCHAR(50)    NOT NULL,
    profit_margin  DECIMAL(5,2),
    sale_price     DECIMAL(10,2),
    average_cost   DECIMAL(10,2),
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT uq_product_variants_product_size_color UNIQUE (product_id, size, color),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_product_variants_stock CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_product_variants_sku        ON product_variants (sku);
CREATE INDEX idx_product_variants_active     ON product_variants (active);
