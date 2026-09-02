# Feature 001 — Autenticação e Usuários

| Campo         | Valor                                |
|---------------|--------------------------------------|
| Branch        | `feature/api-auth-users`             |
| Data          | 2026-09-01                           |
| Módulos       | Scaffold base, `auth`, `user`        |
| Status        | ✅ Concluída e verificada end-to-end |
| Depende de    | —                                    |

---

## Objetivo

Estabelecer a fundação do backend `estilofit-api` e entregar o fluxo completo de autenticação e gerenciamento de usuários, que é pré-requisito para todos os outros módulos (todo endpoint exige usuário autenticado).

---

## Escopo Entregue

### 1. Scaffold e Infraestrutura Base
- Projeto Spring Boot 3.2.5 + Kotlin 1.9.24 + JDK 21
- Gradle Wrapper 8.7 (`build.gradle.kts`, `settings.gradle.kts`)
- `application.yml` com datasource, JPA, Flyway, JWT, CORS, Swagger e logging
- `.env` / `.env.example` com todas as variáveis (ADR-011)
- `docker-compose.yml` com PostgreSQL 15 na raiz do monorepo
- `.gitignore` protegendo segredos e artefatos de build

### 2. Componentes Compartilhados (`shared/`)
- `BaseEntity` — id UUID + timestamps automáticos (`createdAt`, `updatedAt`)
- `PageResponse<T>` — envelope de paginação padronizado (ADR-010)
- Tratamento de erros (ADR-009):
  - `BusinessException`, `EntityNotFoundException`, `DataConflictException`, `InvalidOperationException`
  - `ErrorResponse` + `FieldErrorDetail`
  - `GlobalExceptionHandler` cobrindo 400/401/403/404/409/422/500
- `AppProperties` — binding tipado das configurações (`app.jwt`, `app.cors`, `app.swagger`)
- `WebConfig` — configuração de CORS
- `SwaggerConfig` — OpenAPI com esquema de segurança Bearer JWT
- `DataInitializer` — cria o admin padrão no primeiro boot (evita hash hardcoded)

### 3. Segurança (ADR-005)
- `JwtService` — geração e validação de access/refresh tokens (HS512)
- `JwtAuthenticationFilter` — intercepta o header `Authorization: Bearer`
- `SecurityConfig` — Spring Security stateless, RBAC via `@PreAuthorize`, BCrypt strength 12,
  endpoints públicos (login, refresh, Swagger), handlers customizados de 401 e 403

### 4. Módulo de Autenticação (`auth/`)
- `AuthService` — login (valida senha e status), refresh (com rotação de token)
- `AuthController` — endpoints de login, refresh e logout
- Refresh token entregue como httpOnly cookie (`SameSite=Strict`)

### 5. Módulo de Usuários (`user/`)
- `User` (entity), `Role` (enum: ADMIN, MANAGER, SELLER)
- `UserRepository` com filtros dinâmicos (nome, role, active) e paginação
- `UserService` — CRUD completo + gestão de senha
- `UserController` — endpoints protegidos por perfil

### 6. Banco de Dados — 12 Migrations Flyway
Schema completo do domínio (mesmo módulos ainda sem código Kotlin):

| Migration | Conteúdo |
|-----------|----------|
| V1  | users |
| V2  | categories |
| V3  | products + product_variants |
| V4  | suppliers |
| V5  | supply_lots + supply_lot_items |
| V6  | stock_movements |
| V7  | sale_channels |
| V8  | sales + sale_items |
| V9  | sale_installments |
| V10 | promotion_alerts |
| V11 | system_settings |
| V12 | dados iniciais (categorias, canais, configurações) |

---

## Endpoints Entregues

### Autenticação (`/auth`)
| Método | Rota            | Permissão | Descrição                          |
|--------|-----------------|-----------|------------------------------------|
| POST   | `/auth/login`   | Pública   | Login, retorna accessToken + user  |
| POST   | `/auth/refresh` | Pública   | Renova o access token (cookie)     |
| POST   | `/auth/logout`  | Autenticado | Encerra a sessão                 |

### Usuários (`/users`)
| Método | Rota                    | Permissão | Descrição                     |
|--------|-------------------------|-----------|-------------------------------|
| GET    | `/users`                | 🔴 Admin  | Lista paginada com filtros    |
| POST   | `/users`                | 🔴 Admin  | Cria usuário                  |
| GET    | `/users/{id}`           | 🔴 Admin  | Busca por ID                  |
| PUT    | `/users/{id}`           | 🔴 Admin  | Atualiza dados                |
| PATCH  | `/users/{id}/status`    | 🔴 Admin  | Ativa/desativa                |
| PATCH  | `/users/{id}/password`  | 🔴 Admin  | Redefine senha                |
| GET    | `/users/me`             | 🟢 Todos  | Dados do usuário logado       |
| PATCH  | `/users/me/password`    | 🟢 Todos  | Altera própria senha          |

---

## Decisões Técnicas Relevantes

1. **Flyway 9.x (não 10.x)** — o Spring Boot 3.2.5 gerencia o Flyway 9.22.3, cujo suporte
   a PostgreSQL já vem no `flyway-core`. Não usar o módulo `flyway-database-postgresql`
   (que é do Flyway 10+ e causa `AbstractMethodError` quando misturado com o core 9.x).

2. **Admin criado via `DataInitializer`** — em vez de hash BCrypt hardcoded na migration,
   o admin é criado no primeiro boot com o `PasswordEncoder` real da aplicação. Garante
   o hash correto e permite trocar a senha padrão facilmente.

3. **Access token em memória / refresh token em cookie** — access token vai no body (frontend
   guarda em memória); refresh token vai como httpOnly cookie `SameSite=Strict` para mitigar XSS/CSRF.

4. **Schema completo desde o início** — todas as 12 migrations foram criadas de uma vez para
   que o modelo de domínio esteja consistente, mesmo que o código dos módulos venha depois.

---

## Como Testar

### Pré-requisitos
```bash
# 1. Subir o banco (Docker Desktop precisa estar aberto)
cd estilofit-manager
docker compose up -d

# 2. Subir a aplicação
cd estilofit-api
./gradlew bootRun
```

### Credenciais padrão (criadas no primeiro boot)
```
Email: admin@estilofit.com.br
Senha: admin@123
```

### Via Swagger UI
```
http://localhost:8080/api/v1/swagger-ui/index.html
```
1. `POST /auth/login` com as credenciais acima
2. Copiar o `accessToken`
3. Botão **Authorize** → colar o token
4. Testar os endpoints protegidos

### Via curl
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@estilofit.com.br","password":"admin@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"
```

---

## Verificação Realizada

Testado contra a aplicação rodando com PostgreSQL real:

| Cenário                                  | Resultado esperado | Status |
|------------------------------------------|--------------------|:------:|
| 12 migrations Flyway aplicadas           | Schema criado      | ✅     |
| Login com credenciais válidas            | 200 + JWT          | ✅     |
| `GET /users/me` autenticado              | 200                | ✅     |
| Listagem paginada de usuários            | 200 + envelope     | ✅     |
| Acesso sem token                         | 401                | ✅     |
| Vendedor acessando endpoint de Admin     | 403                | ✅     |
| Criação com dados inválidos              | 400 + fieldErrors  | ✅     |
| Criação com email duplicado              | 409                | ✅     |
| Swagger UI e api-docs                    | 200                | ✅     |

---

## Próxima Feature
`feature/api-categories` — CRUD de categorias (base para o módulo de produtos).
