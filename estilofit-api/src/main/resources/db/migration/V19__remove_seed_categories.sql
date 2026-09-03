-- V19: Remove as categorias de conveniência semeadas na V12.
-- A loja cadastra as próprias categorias do zero. Os canais de venda e as
-- configurações do sistema permanecem (canal é pré-requisito para vender).
--
-- Só remove categorias que ainda não têm produtos vinculados (segurança/idempotência):
-- em bancos onde alguma categoria seed já foi usada, ela é preservada.
DELETE FROM categories c
WHERE c.name IN ('Blusas', 'Calças', 'Vestidos', 'Saias', 'Shorts', 'Conjuntos', 'Leggings', 'Tops')
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id);
