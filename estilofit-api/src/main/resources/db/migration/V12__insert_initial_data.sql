-- V12: Dados iniciais do sistema
-- Obs: o usuário administrador padrão é criado pelo DataInitializer na primeira
-- inicialização da aplicação, para garantir o hash BCrypt correto da senha.

-- ── Categorias padrão ─────────────────────────────────────────────────────
INSERT INTO categories (id, name, active) VALUES
    (gen_random_uuid(), 'Blusas',     TRUE),
    (gen_random_uuid(), 'Calças',     TRUE),
    (gen_random_uuid(), 'Vestidos',   TRUE),
    (gen_random_uuid(), 'Saias',      TRUE),
    (gen_random_uuid(), 'Shorts',     TRUE),
    (gen_random_uuid(), 'Conjuntos',  TRUE),
    (gen_random_uuid(), 'Leggings',   TRUE),
    (gen_random_uuid(), 'Tops',       TRUE);

-- ── Canais de venda padrão ────────────────────────────────────────────────
INSERT INTO sale_channels (id, name, active) VALUES
    (gen_random_uuid(), 'Instagram',  TRUE),
    (gen_random_uuid(), 'WhatsApp',   TRUE),
    (gen_random_uuid(), 'Presencial', TRUE);

-- ── Configurações padrão do sistema ──────────────────────────────────────
INSERT INTO system_settings (id, key, value, description) VALUES
    (gen_random_uuid(), 'DEFAULT_PROFIT_MARGIN',   '100',  'Margem de lucro padrão (%) para cálculo do preço de venda'),
    (gen_random_uuid(), 'PROMOTION_ALERT_DAYS',    '60',   'Dias sem venda para gerar alerta de promoção'),
    (gen_random_uuid(), 'LOW_STOCK_THRESHOLD',     '2',    'Quantidade mínima em estoque antes de exibir alerta');
