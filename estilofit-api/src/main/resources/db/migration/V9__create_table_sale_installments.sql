-- V9: Parcelas de vendas (contas a receber)
CREATE TABLE sale_installments (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    sale_id         UUID          NOT NULL,
    installment_num INTEGER       NOT NULL,
    due_date        DATE          NOT NULL,
    gross_amount    DECIMAL(10,2) NOT NULL,
    net_amount      DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    received_at     TIMESTAMP,
    received_by     UUID,

    CONSTRAINT pk_sale_installments PRIMARY KEY (id),
    CONSTRAINT uq_sale_installments_sale_num UNIQUE (sale_id, installment_num),
    CONSTRAINT fk_sale_installments_sale        FOREIGN KEY (sale_id)     REFERENCES sales (id),
    CONSTRAINT fk_sale_installments_received_by FOREIGN KEY (received_by) REFERENCES users (id),
    CONSTRAINT ck_sale_installments_status      CHECK (status IN ('PENDING', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_sale_installments_num         CHECK (installment_num > 0)
);

CREATE INDEX idx_sale_installments_sale_id  ON sale_installments (sale_id);
CREATE INDEX idx_sale_installments_due_date ON sale_installments (due_date);
CREATE INDEX idx_sale_installments_status   ON sale_installments (status);
