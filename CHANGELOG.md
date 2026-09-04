# Changelog

Todas as mudanças relevantes do EstiloFit Manager.

## [1.2.1] — 2026-09-03

### Corrigido
- **Preço manual inacessível na tela de Variações** — o modal era estreito e cortava a
  coluna de ações, escondendo o botão de editar preço. O modal ficou mais largo e a tabela
  passou a rolar horizontalmente, deixando o botão sempre visível em cada variação.

### Adicionado
- **Script de atualização no Windows** (`scripts/update.bat`) — atualiza preservando os
  dados (backup + git pull + rebuild, sem apagar o volume). Ver `scripts/README.md`.

## [1.2.0] — 2026-09-02

### Adicionado
- **Preço de venda manual por variação** — o preço pela margem passa a ser uma sugestão.
  É possível fixar um preço acima ou abaixo do sugerido; a entrada de mercadoria continua
  atualizando o custo médio, mas não altera o preço manual. Botão "voltar ao sugerido"
  recalcula pela margem. Preço abaixo do custo é permitido (aviso visual, sem bloqueio).
  Não afeta venda/lucro/comissão (já usam o preço gravado). Ver `docs/features/022-manual-sale-price.md`.

## [1.1.0] — 2026-09-02

Foco em uso local (notebook Windows), robustez do build e operação.

### Adicionado
- **Base limpa para uso real** — instalação nova nasce apenas com o usuário admin,
  os canais de venda e as configurações do sistema; categorias/produtos/etc. são
  cadastrados do zero (migration V19).
- **Guia de uso local no Windows** (`docs/local-notebook.md`) — passo a passo com
  Docker Desktop, subir no boot do Windows e acesso na rede local.
- **Backup do banco no Windows** — `scripts/backup.bat` (dump datado, retenção dos 30
  mais recentes) e `scripts/README.md` com o agendamento via Agendador de Tarefas.
- **CI/CD (GitHub Actions)** — workflow de CI (testes backend + build/typecheck frontend)
  e de Release (publica imagens no GHCR na tag `v*`); ver `docs/ci-cd.md`.

### Corrigido
- **Build no Windows** — clone com CRLF quebrava o `gradlew` no container
  (`./gradlew: not found`); normalização no Dockerfile + `.gitattributes` (LF no wrapper).
- **Memória do build** — limite de heap e Kotlin in-process para evitar OOM no Docker
  Desktop do Windows.

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
