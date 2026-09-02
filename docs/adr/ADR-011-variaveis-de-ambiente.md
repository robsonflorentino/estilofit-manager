# ADR-011 — Variáveis de Ambiente

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema possui configurações que variam entre ambientes (desenvolvimento, produção) e configurações sensíveis (senhas, chaves secretas) que não devem ser versionadas no Git. É necessário um padrão claro de como essas configurações são gerenciadas.

## Decisão
Utilizar arquivos `.env` para desenvolvimento local e variáveis de ambiente do sistema operacional/plataforma de deploy para produção. Nunca versionar arquivos `.env` com valores reais.

---

## Convenção de Arquivos

| Arquivo             | Versionado? | Descrição                                              |
|---------------------|:-----------:|--------------------------------------------------------|
| `.env.example`      | ✅ Sim      | Template com todas as variáveis e valores fictícios    |
| `.env`              | ❌ Não      | Valores reais de desenvolvimento local (no .gitignore) |
| `.env.production`   | ❌ Não      | Nunca existe em arquivo — usadas direto na plataforma  |

---

## Backend — `estilofit-api`

### Arquivo `.env.example`
```properties
# ── Aplicação ──────────────────────────────────────────
APP_PORT=8080
APP_ENV=development

# ── Banco de Dados ─────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=estilofit_manager
DB_USERNAME=estilofit
DB_PASSWORD=estilofit123

# ── JWT ────────────────────────────────────────────────
# Gerar com: openssl rand -base64 64
JWT_SECRET=your-super-secret-key-change-in-production
JWT_ACCESS_TOKEN_EXPIRATION=28800
JWT_REFRESH_TOKEN_EXPIRATION=604800

# ── CORS ───────────────────────────────────────────────
CORS_ALLOWED_ORIGINS=http://localhost:5173

# ── Swagger ────────────────────────────────────────────
# false em produção
SWAGGER_ENABLED=true

# ── Logs ───────────────────────────────────────────────
LOG_LEVEL=INFO
```

### Mapeamento no `application.yml`
```yaml
server:
  port: ${APP_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:estilofit_manager}
    username: ${DB_USERNAME:estilofit}
    password: ${DB_PASSWORD:estilofit123}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:28800}
    refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  swagger:
    enabled: ${SWAGGER_ENABLED:true}

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    br.com.estilofitudi: DEBUG
```

---

## Frontend — `estilofit-web`

### Arquivo `.env.example`
```properties
# URL base da API (sem barra no final)
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Nome da aplicação (usado no <title> e cabeçalhos)
VITE_APP_NAME=EstiloFit Manager
```

### Uso no código (Vite)
```typescript
// src/lib/api.ts
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})
```

> Variáveis Vite **devem** ter o prefixo `VITE_` para serem expostas ao bundle.
> Nunca colocar secrets no frontend — qualquer valor aqui é público.

---

## Variáveis Críticas de Segurança

| Variável     | Onde usar       | Como gerar                          | Observação                                    |
|--------------|-----------------|-------------------------------------|-----------------------------------------------|
| `JWT_SECRET` | Backend         | `openssl rand -base64 64`           | Mínimo 32 caracteres; diferente por ambiente  |
| `DB_PASSWORD`| Backend         | Senha forte aleatória               | Nunca usar o padrão `estilofit123` em produção|

---

## Regras

1. **Nunca versionar** `.env` com valores reais — o `.gitignore` deve bloqueá-los
2. **Sempre manter** `.env.example` atualizado quando uma nova variável for adicionada
3. **Em produção**, as variáveis são configuradas diretamente na plataforma de deploy (painel do Railway, AWS Parameter Store, etc.) — sem arquivo `.env`
4. **Valores padrão** no `application.yml` só devem existir para desenvolvimento — produção sempre exige a variável explícita
5. **`JWT_SECRET`** deve ser gerado com `openssl rand -base64 64` e nunca reutilizado entre ambientes

## Consequências
- Novos desenvolvedores precisam copiar `.env.example` para `.env` e preencher os valores antes de rodar o projeto
- O README documenta esse passo explicitamente
- Rotação do `JWT_SECRET` invalida todos os tokens ativos — planejar com antecedência em produção
