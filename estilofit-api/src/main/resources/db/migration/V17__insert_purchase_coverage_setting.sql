-- V17: Cobertura de estoque desejada (dias) — base da sugestão de compra do próximo lote
INSERT INTO system_settings (id, key, value, description) VALUES
    (gen_random_uuid(), 'PURCHASE_COVERAGE_DAYS', '30', 'Dias de cobertura de estoque desejados para a sugestão de compra')
ON CONFLICT (key) DO NOTHING;
