-- V6: Movimentações de estoque
CREATE TABLE stock_movements (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    variant_id     UUID        NOT NULL,
    type           VARCHAR(20) NOT NULL,
    quantity       INTEGER     NOT NULL,
    reference_type VARCHAR(20),
    reference_id   UUID,
    notes          TEXT,
    user_id        UUID        NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_stock_movements_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT ck_stock_movements_type    CHECK (type IN ('ENTRY', 'SALE', 'ADJUSTMENT'))
);

CREATE INDEX idx_stock_movements_variant_id  ON stock_movements (variant_id);
CREATE INDEX idx_stock_movements_type        ON stock_movements (type);
CREATE INDEX idx_stock_movements_created_at  ON stock_movements (created_at);
CREATE INDEX idx_stock_movements_reference   ON stock_movements (reference_type, reference_id);
