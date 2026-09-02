-- V8: Vendas e itens de venda
CREATE TABLE sales (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    channel_id       UUID           NOT NULL,
    seller_id        UUID           NOT NULL,
    confirmed_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    total_amount     DECIMAL(10,2)  NOT NULL,
    discount_amount  DECIMAL(10,2)  NOT NULL DEFAULT 0,
    final_amount     DECIMAL(10,2)  NOT NULL,
    payment_method   VARCHAR(20)    NOT NULL,
    installments     INTEGER        NOT NULL DEFAULT 1,
    card_fee_pct     DECIMAL(5,2),
    card_fee_passed  BOOLEAN                 DEFAULT TRUE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    notes            TEXT,
    cancelled_by     UUID,
    cancelled_at     TIMESTAMP,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_sales PRIMARY KEY (id),
    CONSTRAINT fk_sales_channel      FOREIGN KEY (channel_id)   REFERENCES sale_channels (id),
    CONSTRAINT fk_sales_seller       FOREIGN KEY (seller_id)    REFERENCES users (id),
    CONSTRAINT fk_sales_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT ck_sales_payment      CHECK (payment_method IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD')),
    CONSTRAINT ck_sales_status       CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_sales_installments CHECK (installments >= 1),
    CONSTRAINT ck_sales_discount     CHECK (discount_amount >= 0),
    CONSTRAINT ck_sales_final        CHECK (final_amount > 0)
);

CREATE INDEX idx_sales_channel_id    ON sales (channel_id);
CREATE INDEX idx_sales_seller_id     ON sales (seller_id);
CREATE INDEX idx_sales_status        ON sales (status);
CREATE INDEX idx_sales_confirmed_at  ON sales (confirmed_at);
CREATE INDEX idx_sales_payment       ON sales (payment_method);

CREATE TABLE sale_items (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    sale_id     UUID          NOT NULL,
    variant_id  UUID          NOT NULL,
    quantity    INTEGER       NOT NULL,
    unit_price  DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,

    CONSTRAINT pk_sale_items PRIMARY KEY (id),
    CONSTRAINT fk_sale_items_sale    FOREIGN KEY (sale_id)    REFERENCES sales (id),
    CONSTRAINT fk_sale_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_sale_items_qty     CHECK (quantity > 0),
    CONSTRAINT ck_sale_items_price   CHECK (unit_price > 0)
);

CREATE INDEX idx_sale_items_sale_id    ON sale_items (sale_id);
CREATE INDEX idx_sale_items_variant_id ON sale_items (variant_id);
