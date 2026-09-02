-- V18: Frete na venda (repasse ao cliente — não entra em faturamento/lucro/comissão)
ALTER TABLE sales ADD COLUMN freight_type   VARCHAR(10)   NOT NULL DEFAULT 'NONE';
ALTER TABLE sales ADD COLUMN freight_amount DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE sales ADD CONSTRAINT ck_sales_freight_type
    CHECK (freight_type IN ('NONE', 'FREE', 'PAID'));
ALTER TABLE sales ADD CONSTRAINT ck_sales_freight_amount
    CHECK (freight_amount >= 0);
