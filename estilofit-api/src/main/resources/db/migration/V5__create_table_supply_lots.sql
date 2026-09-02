-- V5: Lotes de entrada de mercadoria
CREATE TABLE supply_lots (
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    supplier_id  UUID           NOT NULL,
    received_at  DATE           NOT NULL,
    freight_cost DECIMAL(10,2)  NOT NULL DEFAULT 0,
    total_cost   DECIMAL(10,2)  NOT NULL,
    notes        TEXT,
    created_by   UUID           NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_supply_lots PRIMARY KEY (id),
    CONSTRAINT fk_supply_lots_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_supply_lots_user     FOREIGN KEY (created_by)  REFERENCES users (id)
);

CREATE INDEX idx_supply_lots_supplier_id  ON supply_lots (supplier_id);
CREATE INDEX idx_supply_lots_received_at  ON supply_lots (received_at);
CREATE INDEX idx_supply_lots_created_by   ON supply_lots (created_by);

CREATE TABLE supply_lot_items (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    lot_id         UUID          NOT NULL,
    variant_id     UUID          NOT NULL,
    quantity       INTEGER       NOT NULL,
    unit_cost      DECIMAL(10,2) NOT NULL,
    freight_share  DECIMAL(10,2) NOT NULL DEFAULT 0,
    real_unit_cost DECIMAL(10,2) NOT NULL,

    CONSTRAINT pk_supply_lot_items PRIMARY KEY (id),
    CONSTRAINT fk_supply_lot_items_lot     FOREIGN KEY (lot_id)     REFERENCES supply_lots (id),
    CONSTRAINT fk_supply_lot_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_supply_lot_items_qty     CHECK (quantity > 0),
    CONSTRAINT ck_supply_lot_items_cost    CHECK (unit_cost > 0)
);

CREATE INDEX idx_supply_lot_items_lot_id     ON supply_lot_items (lot_id);
CREATE INDEX idx_supply_lot_items_variant_id ON supply_lot_items (variant_id);
