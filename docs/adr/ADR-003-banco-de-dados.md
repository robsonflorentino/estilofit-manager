# ADR-003 — Banco de Dados: PostgreSQL

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema precisa de um banco de dados relacional para persistir produtos, variações, estoque, vendas, fornecedores e usuários, com suporte a transações ACID para garantir consistência do estoque.

## Decisão
Utilizar **PostgreSQL 15+** como banco de dados principal.

## Justificativa
- Banco relacional maduro e de alta confiabilidade
- Suporte completo a transações ACID — essencial para movimentações de estoque
- Excelente suporte a JSON para campos semi-estruturados (ex: metadados de variações)
- Amplamente suportado por provedores de cloud (AWS RDS, Railway, Supabase, Render)
- Gratuito e open source
- Integração nativa com Spring Data JPA / Hibernate

## Consequências
- Necessário instância PostgreSQL em ambiente de desenvolvimento (via Docker Compose)
- Migrações de schema gerenciadas pelo **Flyway** para rastreabilidade e rollback controlado
- Dados relacionais bem definidos — alterações no modelo de domínio requerem migrations
