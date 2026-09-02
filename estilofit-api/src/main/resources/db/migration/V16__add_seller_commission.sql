-- V16: Comissão do vendedor (snapshot por venda)

-- Configuração global do percentual de comissão (padrão 5%)
INSERT INTO system_settings (id, key, value, description) VALUES
    (gen_random_uuid(), 'SELLER_COMMISSION_PCT', '5', 'Percentual de comissão do vendedor sobre o faturamento da venda')
ON CONFLICT (key) DO NOTHING;

-- Snapshot da comissão em cada venda
ALTER TABLE sales ADD COLUMN commission_pct    DECIMAL(5,2)  NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN commission_amount DECIMAL(10,2) NOT NULL DEFAULT 0;

-- Backfill: vendas confirmadas feitas por usuários com papel SELLER assumem a taxa
-- histórica de 5% (as demais permanecem com comissão zero).
UPDATE sales s
SET commission_pct = 5,
    commission_amount = ROUND(s.final_amount * 0.05, 2)
FROM users u
WHERE s.seller_id = u.id
  AND u.role = 'SELLER'
  AND s.status = 'CONFIRMED';
