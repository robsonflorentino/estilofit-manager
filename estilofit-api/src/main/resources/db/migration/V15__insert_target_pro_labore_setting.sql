-- V15: Pró-labore desejado (R$/mês) — base para a meta de vendas mensal
INSERT INTO system_settings (id, key, value, description) VALUES
    (gen_random_uuid(), 'TARGET_PRO_LABORE', '3000', 'Pró-labore desejado por mês (R$), usado para calcular a meta de vendas')
ON CONFLICT (key) DO NOTHING;
