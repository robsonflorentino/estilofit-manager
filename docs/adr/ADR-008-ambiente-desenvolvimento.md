# ADR-008 — Ambiente de Desenvolvimento: Docker Compose

## Status
Aceito

## Data
2026-09-01

## Contexto
Para padronizar o ambiente de desenvolvimento e evitar o problema de "funciona na minha máquina", os serviços de infraestrutura (banco de dados) devem ser executados de forma isolada e reproduzível.

## Decisão
Utilizar **Docker Compose** para orquestrar os serviços de infraestrutura em ambiente de desenvolvimento.

## Serviços no docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: estilofit_manager
      POSTGRES_USER: estilofit
      POSTGRES_PASSWORD: estilofit123
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

## Justificativa
- Ambiente idêntico para todos os desenvolvedores
- Fácil reset do banco de dados em desenvolvimento
- Sem necessidade de instalar PostgreSQL localmente
- Preparação natural para deploy em containers no futuro

## Consequências
- Docker e Docker Compose devem estar instalados na máquina do desenvolvedor
- O backend em desenvolvimento roda diretamente na JVM (não em container), apontando para o PostgreSQL do Docker
- O frontend em desenvolvimento roda com `npm run dev` apontando para a API local
