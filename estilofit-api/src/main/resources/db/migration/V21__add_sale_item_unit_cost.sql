-- V21: Snapshot do custo unitário no item de venda.
-- Antes, o lucro das vendas era calculado com o custo médio ATUAL da variação, então
-- corrigir o custo depois alterava o lucro de vendas passadas. Agora cada item de venda
-- congela o custo do momento da venda (unit_cost), e os relatórios passam a usar esse valor.

-- 1) coluna nova (temporariamente aceitando nulo para permitir o backfill)
ALTER TABLE sale_items ADD COLUMN unit_cost DECIMAL(10,2);

-- 2) backfill das vendas existentes: usa o custo médio ATUAL da variação como melhor
--    aproximação do custo histórico (não havia snapshot). COALESCE para variações sem custo.
UPDATE sale_items si
SET unit_cost = COALESCE(
    (SELECT pv.average_cost FROM product_variants pv WHERE pv.id = si.variant_id),
    0
);

-- 3) a partir de agora a coluna é obrigatória (default 0 por segurança)
ALTER TABLE sale_items ALTER COLUMN unit_cost SET DEFAULT 0;
ALTER TABLE sale_items ALTER COLUMN unit_cost SET NOT NULL;
