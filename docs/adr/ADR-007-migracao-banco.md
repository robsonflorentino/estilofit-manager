# ADR-007 — Migrations de Banco de Dados: Flyway

## Status
Aceito

## Data
2026-09-01

## Contexto
O schema do banco de dados vai evoluir ao longo do desenvolvimento. É necessário uma ferramenta que versione e aplique as mudanças de forma controlada e rastreável em todos os ambientes (desenvolvimento, homologação, produção).

## Decisão
Utilizar **Flyway** para gerenciamento de migrations.

## Convenção de nomenclatura dos arquivos
```
V{versão}__{descrição}.sql

Exemplos:
V1__create_table_users.sql
V2__create_table_products.sql
V3__create_table_product_variants.sql
V4__add_column_promotion_threshold_days.sql
```

## Localização
```
estilofit-api/src/main/resources/db/migration/
```

## Justificativa
- Integração nativa com Spring Boot (auto-execução no startup)
- Histórico de mudanças no schema versionado junto ao código-fonte (Git)
- Rollback controlado com scripts de undo quando necessário
- Amplamente adotado no ecossistema Spring

## Consequências
- Todo DDL (CREATE TABLE, ALTER TABLE, etc.) deve ser feito via migration, nunca manualmente no banco
- Em ambiente de desenvolvimento, o banco pode ser recriado do zero rodando todas as migrations em sequência
- Nunca editar uma migration já aplicada — sempre criar uma nova
