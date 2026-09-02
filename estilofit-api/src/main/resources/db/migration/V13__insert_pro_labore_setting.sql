-- V13: Configuração do percentual de pró-labore (KPI estimado do dashboard)
-- Percentual aplicado sobre o lucro do mês (faturamento - custo das mercadorias vendidas).
INSERT INTO system_settings (id, key, value, description) VALUES
    (gen_random_uuid(), 'PRO_LABORE_PCT', '30', 'Percentual (%) do lucro destinado ao pró-labore estimado')
ON CONFLICT (key) DO NOTHING;
