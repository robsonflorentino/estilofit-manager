-- V7: Canais de venda
CREATE TABLE sale_channels (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_sale_channels PRIMARY KEY (id),
    CONSTRAINT uq_sale_channels_name UNIQUE (name)
);

CREATE INDEX idx_sale_channels_active ON sale_channels (active);
