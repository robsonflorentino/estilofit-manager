-- V20: Preço de venda manual por variação.
-- Quando price_override = TRUE, o preço de venda foi definido manualmente e NÃO deve
-- ser sobrescrito pelo cálculo por margem na entrada de mercadoria (o custo médio
-- continua sendo recalculado normalmente). O preço por margem passa a ser apenas sugerido.
ALTER TABLE product_variants
    ADD COLUMN price_override BOOLEAN NOT NULL DEFAULT FALSE;
