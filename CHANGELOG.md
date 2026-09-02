# Changelog

Todas as mudanças relevantes do EstiloFit Manager.

## [1.0.0] — 2026-09-02

Primeiro release estável, com o sistema completo e pronto para produção.

### Plataforma
- Backend Kotlin + Spring Boot 3.2 (Java 21), PostgreSQL + Flyway
- Frontend React + TypeScript + Vite
- Autenticação JWT (access token em memória + refresh token em cookie httpOnly)
- Perfis de acesso: Admin, Gestor e Vendedor (RBAC)
- 153 testes automatizados (unitários e de integração com Testcontainers)
- Pacote de deploy Docker (API + frontend/nginx + Postgres) — ver `docs/deploy.md`

### Funcionalidades
- **Autenticação e usuários** — login, refresh, gestão de usuários e papéis
- **Catálogo** — categorias, produtos e variações (tamanho/cor, SKU)
- **Estoque** — entrada de mercadoria (lote com rateio de frete e custo médio), consulta e ajuste manual
- **Fornecedores**
- **Vendas** — venda atômica com baixa de estoque, snapshot de preço, desconto, formas de pagamento e parcelamento no cartão
- **Contas a Receber** — parcelas, baixa e fluxo de caixa projetado
- **Comissão do vendedor** — percentual configurável com snapshot por venda; tela de comissões a pagar
- **Frete na venda** — sem frete / grátis / pago (repasse, não entra em faturamento/lucro)
- **Dashboard** — KPIs do mês, meta de pró-labore e lucro por canal
- **Relatórios** — faturamento por dia, top produtos, por canal, por forma de pagamento, meta de vendas para o pró-labore, lucratividade por canal, ranking de vendedores (pódio) e sugestão de compra do próximo lote (agrupada por fornecedor)
- **Alertas de promoção** — produtos parados (sem venda há X dias)
- **Configurações** — parâmetros do sistema editáveis (margem, estoque baixo, pró-labore, comissão, dias de alerta, cobertura de compra)

### Notas de operação
- Em produção, rodar com `COOKIE_SECURE=true` sob HTTPS.
- Em desenvolvimento local (HTTP), usar `COOKIE_SECURE=false`.
