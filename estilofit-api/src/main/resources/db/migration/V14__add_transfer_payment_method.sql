-- V14: inclui TRANSFER (transferência) como forma de pagamento válida
ALTER TABLE sales DROP CONSTRAINT ck_sales_payment;
ALTER TABLE sales ADD CONSTRAINT ck_sales_payment
    CHECK (payment_method IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD', 'TRANSFER'));
