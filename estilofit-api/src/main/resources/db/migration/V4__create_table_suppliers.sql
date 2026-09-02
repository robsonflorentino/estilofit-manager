-- V4: Fornecedores
CREATE TABLE suppliers (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(200),
    whatsapp      VARCHAR(20),
    cnpj          VARCHAR(18),
    address       TEXT,
    notes         TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT uq_suppliers_cnpj UNIQUE (cnpj)
);

CREATE INDEX idx_suppliers_active ON suppliers (active);
