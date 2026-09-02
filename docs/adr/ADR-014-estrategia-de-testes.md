# ADR-014 — Estratégia de Testes do Backend

## Status
Aceito

## Data
2026-09-01

## Contexto
O backend precisa de testes automatizados para proteger contra regressão e permitir evolução segura a cada nova feature. Até então a validação era manual (curl contra a aplicação rodando), o que não fica versionado nem roda de forma repetível.

## Decisão
Adotar dois níveis de teste automatizado:

1. **Testes unitários** — com **JUnit 5 + MockK**, focados nas regras de negócio dos services (repositórios e dependências mockados). Rápidos, sem infraestrutura.
2. **Testes de integração** — com **@SpringBootTest + MockMvc + Testcontainers**, exercitando o fluxo completo (controller → service → repository → banco) contra um **PostgreSQL real e efêmero**.

---

## Por que Testcontainers (e não H2)

O projeto usa recursos específicos do PostgreSQL nas migrations (`gen_random_uuid()`, `to_tsvector('portuguese', ...)`, tipos `UUID`, constraints `CHECK`). O H2 não suporta parte disso e exigiria migrations separadas para teste — testando contra um banco diferente do de produção.

O Testcontainers sobe o **mesmo PostgreSQL 15** usado em produção, com as mesmas migrations Flyway. Se passa no teste, há confiança real de que funciona em produção. O custo (Docker rodando durante os testes) já está resolvido no ambiente.

---

## Bibliotecas

| Biblioteca                          | Versão   | Uso                                    |
|-------------------------------------|----------|----------------------------------------|
| `spring-boot-starter-test`          | (gerenciada) | JUnit 5, AssertJ, MockMvc          |
| `spring-security-test`              | (gerenciada) | Suporte a testes com segurança     |
| `io.mockk:mockk`                    | 1.13.10  | Mocking idiomático em Kotlin           |
| `org.testcontainers:junit-jupiter`  | 1.19.7   | Integração Testcontainers + JUnit 5    |
| `org.testcontainers:postgresql`     | 1.19.7   | Container PostgreSQL efêmero           |

---

## Estrutura de Testes

```
src/test/kotlin/br/com/estilofitudi/
├── support/
│   ├── IntegrationTest.kt      ← classe base: @SpringBootTest + Testcontainers
│   └── TestAuthHelper.kt       ← cria usuários por perfil e gera tokens JWT
├── auth/
│   ├── service/AuthServiceTest.kt      (unitário)
│   └── AuthIntegrationTest.kt          (integração)
├── user/
│   ├── service/UserServiceTest.kt      (unitário)
│   └── UserIntegrationTest.kt          (integração)
└── category/
    ├── service/CategoryServiceTest.kt  (unitário)
    └── CategoryIntegrationTest.kt      (integração)
```

### Classe base de integração
- `PostgreSQLContainer("postgres:15-alpine")` estático e reutilizado entre classes (singleton) — evita subir um Postgres por classe
- `@DynamicPropertySource` injeta URL/usuário/senha do container no Spring
- Profile `test` (`application-test.yml`) com `JWT_SECRET` fixo e Swagger desabilitado
- Flyway aplica as migrations no container antes dos testes

### Helper de autenticação
`TestAuthHelper` cria usuários de cada perfil (ADMIN, MANAGER, SELLER) e gera tokens JWT válidos, evitando repetir o fluxo de login em cada teste de integração.

---

## Convenção de Cobertura

Meta pragmática adotada:

- **Services** → teste unitário obrigatório (regras de negócio, casos de erro)
- **Endpoints** → teste de integração cobrindo o caminho feliz + os principais erros
  (401 sem token, 403 sem permissão, 409 conflito, 422 regra de negócio, 400 validação)
- **Lógica não-trivial isolada** (ex.: futuro `SkuGenerator`) → teste unitário dedicado

---

## Como Rodar

```bash
# Docker precisa estar rodando (Testcontainers)
cd estilofit-api
./gradlew test
```

Relatório HTML gerado em `build/reports/tests/test/index.html`.

---

## Estado Atual (retrofit inicial)

Cobertura das features já entregues (001-auth-users, 002-categories):

| Classe                   | Tipo       | Testes |
|--------------------------|------------|:------:|
| AuthServiceTest          | unitário   | 7      |
| UserServiceTest          | unitário   | 7      |
| CategoryServiceTest      | unitário   | 8      |
| AuthIntegrationTest      | integração | 4      |
| UserIntegrationTest      | integração | 8      |
| CategoryIntegrationTest  | integração | 8      |
| **Total**                |            | **42** |

Todos passando (0 falhas, 0 erros).

---

## Consequências
- Toda feature nova deve vir com testes (unitário nos services + integração nos endpoints)
- O CI futuro (GitHub Actions) precisará de Docker disponível para o Testcontainers — é suportado nativamente
- O primeiro `./gradlew test` de cada ambiente baixa a imagem `postgres:15-alpine` (cacheada nas execuções seguintes)
- Testes de integração são mais lentos que unitários; manter o container como singleton mitiga o custo
