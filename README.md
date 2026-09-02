# EstiloFit Manager

Sistema de gestão de estoque e vendas da loja **EstiloFit — Moda Fitness**.

Permite gerenciar produtos, variações (tamanho/cor), fornecedores, entradas de mercadoria, vendas, contas a receber e relatórios de negócio, incluindo estimativa de pró-labore mensal.

---

## Índice

- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do Ambiente](#configuração-do-ambiente)
- [Rodando Localmente](#rodando-localmente)
- [Comandos Úteis](#comandos-úteis)
- [Documentação](#documentação)
- [Perfis de Acesso](#perfis-de-acesso)
- [Estrutura de Pastas](#estrutura-de-pastas)

---

## Estrutura do Projeto

```
estilofit-manager/
├── estilofit-api/        ← Backend (Kotlin + Spring Boot 3)
├── estilofit-web/        ← Frontend (React + TypeScript + Tailwind)
└── docs/
    ├── adr/              ← Architecture Decision Records
    ├── business/         ← Regras de negócio, modelo de domínio, design system
    └── openapi.yaml      ← Contrato da API (OpenAPI 3.0)
```

---

## Pré-requisitos

Certifique-se de ter instalado:

| Ferramenta     | Versão mínima | Como verificar          |
|----------------|:-------------:|-------------------------|
| JDK            | 21            | `java -version`         |
| Gradle         | 8.x           | `./gradlew --version`   |
| Node.js        | 20            | `node --version`        |
| npm            | 10            | `npm --version`         |
| Docker         | 24+           | `docker --version`      |
| Docker Compose | 2.x           | `docker compose version`|

---

## Configuração do Ambiente

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/estilofit-manager.git
cd estilofit-manager
```

### 2. Configure o Backend

```bash
cd estilofit-api

# Copie o arquivo de variáveis de ambiente
cp .env.example .env

# Edite o .env com suas configurações locais
# Os valores padrão já funcionam com o Docker Compose local
```

Gere um `JWT_SECRET` seguro:
```bash
openssl rand -base64 64
```

Cole o valor gerado na variável `JWT_SECRET` do `.env`.

### 3. Configure o Frontend

```bash
cd estilofit-web

# Copie o arquivo de variáveis de ambiente
cp .env.example .env

# O valor padrão já aponta para o backend local — nenhuma alteração necessária
```

---

## Rodando Localmente

### Passo 1 — Suba o banco de dados

Na raiz do projeto:

```bash
docker compose up -d
```

Isso sobe um container PostgreSQL na porta `5432` com o banco `estilofit_manager`.

Verifique se está rodando:
```bash
docker compose ps
```

### Passo 2 — Suba o Backend

```bash
cd estilofit-api
./gradlew bootRun
```

O backend estará disponível em `http://localhost:8080`.

As migrations do Flyway são executadas automaticamente na inicialização.

### Passo 3 — Suba o Frontend

```bash
cd estilofit-web
npm install
npm run dev
```

O frontend estará disponível em `http://localhost:5173`.

### Primeiro Acesso

Após subir os serviços, acesse `http://localhost:5173` e faça login com o usuário administrador criado pela migration inicial:

```
Email:  admin@estilofit.com.br
Senha:  admin@123
```

> ⚠️ Troque a senha no primeiro acesso.

---

## Comandos Úteis

### Backend

```bash
# Rodar em modo desenvolvimento (com hot reload)
./gradlew bootRun

# Compilar sem rodar
./gradlew build

# Rodar testes
./gradlew test

# Gerar JAR para produção
./gradlew bootJar

# Ver migrations pendentes
./gradlew flywayInfo

# Executar migrations manualmente
./gradlew flywayMigrate
```

### Frontend

```bash
# Instalar dependências
npm install

# Rodar em desenvolvimento
npm run dev

# Build de produção
npm run build

# Preview do build de produção localmente
npm run preview

# Verificar erros de lint
npm run lint

# Verificar tipos TypeScript
npm run type-check
```

### Docker

```bash
# Subir banco de dados
docker compose up -d

# Parar banco de dados
docker compose down

# Parar e apagar volume (zera o banco)
docker compose down -v

# Ver logs do banco
docker compose logs postgres -f

# Conectar ao banco via psql
docker compose exec postgres psql -U estilofit -d estilofit_manager
```

### Backup do Banco (Produção)

```bash
# Gerar backup
pg_dump \
  --host=<DB_HOST> --port=<DB_PORT> \
  --username=<DB_USERNAME> --dbname=<DB_NAME> \
  --format=custom --compress=9 \
  --file="estilofit_backup_$(date +%Y%m%d_%H%M%S).dump"

# Restaurar backup
pg_restore \
  --host=<DB_HOST> --port=<DB_PORT> \
  --username=<DB_USERNAME> --dbname=<DB_NAME> \
  --clean --if-exists \
  estilofit_backup_YYYYMMDD_HHMMSS.dump
```

---

## Documentação

| Documento                  | Localização                                    |
|----------------------------|------------------------------------------------|
| Regras de Negócio          | `docs/business/regras-de-negocio.md`           |
| Modelo de Domínio          | `docs/business/modelo-de-dominio.md`           |
| Design System              | `docs/business/design-system.md`               |
| Contrato da API (Markdown) | `docs/business/api-contract.md`                |
| Contrato da API (OpenAPI)  | `docs/openapi.yaml`                            |
| Swagger UI (local)         | `http://localhost:8080/swagger-ui.html`        |
| ADR-001 — Linguagem        | `docs/adr/ADR-001-linguagem-backend.md`        |
| ADR-002 — Framework        | `docs/adr/ADR-002-framework-backend.md`        |
| ADR-003 — Banco de Dados   | `docs/adr/ADR-003-banco-de-dados.md`           |
| ADR-004 — Arquitetura      | `docs/adr/ADR-004-arquitetura-backend.md`      |
| ADR-005 — Autenticação     | `docs/adr/ADR-005-autenticacao.md`             |
| ADR-006 — Frontend         | `docs/adr/ADR-006-frontend.md`                 |
| ADR-007 — Migrations       | `docs/adr/ADR-007-migracao-banco.md`           |
| ADR-008 — Ambiente Dev     | `docs/adr/ADR-008-ambiente-desenvolvimento.md` |
| ADR-009 — Erros            | `docs/adr/ADR-009-tratamento-de-erros.md`      |
| ADR-010 — Paginação        | `docs/adr/ADR-010-paginacao-e-filtros.md`      |
| ADR-011 — Variáveis de Env | `docs/adr/ADR-011-variaveis-de-ambiente.md`    |
| ADR-012 — Deploy           | `docs/adr/ADR-012-estrategia-de-deploy.md`     |
| ADR-013 — Backup           | `docs/adr/ADR-013-politica-de-backup.md`       |

---

## Perfis de Acesso

| Perfil     | Responsabilidade                        | Acesso                                              |
|------------|-----------------------------------------|-----------------------------------------------------|
| `ADMIN`    | Administrador do sistema                | Total — usuários, configurações técnicas e negócio  |
| `MANAGER`  | Gestor / Proprietária da loja           | Operação completa do negócio + todos os relatórios  |
| `SELLER`   | Vendedor                                | Registrar vendas, consultar estoque, ver próprias vendas |

---

## Estrutura de Pastas

### Backend (`estilofit-api`)

```
estilofit-api/
├── src/
│   ├── main/
│   │   ├── kotlin/br/com/estilofitudi/
│   │   │   ├── auth/           ← Autenticação JWT
│   │   │   ├── product/        ← Produtos e variações
│   │   │   ├── category/       ← Categorias
│   │   │   ├── supplier/       ← Fornecedores
│   │   │   ├── inventory/      ← Lotes de entrada e movimentações
│   │   │   ├── sale/           ← Vendas e itens
│   │   │   ├── installment/    ← Parcelas / contas a receber
│   │   │   ├── promotion/      ← Alertas de promoção
│   │   │   ├── report/         ← Relatórios e indicadores
│   │   │   ├── user/           ← Usuários e perfis
│   │   │   ├── settings/       ← Configurações do sistema
│   │   │   └── shared/         ← Exceções, DTOs e utilitários compartilhados
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/   ← Scripts Flyway (V1__, V2__, ...)
│   └── test/
├── Dockerfile
├── .env.example
└── build.gradle.kts
```

### Frontend (`estilofit-web`)

```
estilofit-web/
├── src/
│   ├── assets/         ← Imagens, logos, fontes
│   ├── components/     ← Componentes reutilizáveis (Button, Table, Modal...)
│   ├── hooks/          ← Custom hooks (usePagination, useFilters, useAuth...)
│   ├── layouts/        ← AppLayout (sidebar + topbar), AuthLayout
│   ├── lib/            ← Configuração do Axios, React Query
│   ├── pages/          ← Páginas por módulo (Dashboard, Products, Sales...)
│   ├── routes/         ← Configuração de rotas e guards (PrivateRoute, RoleRoute)
│   ├── services/       ← Chamadas à API por módulo
│   ├── store/          ← Estado global (Zustand) — auth, user
│   ├── types/          ← Tipos TypeScript compartilhados
│   └── utils/          ← Formatadores, helpers
├── Dockerfile
├── nginx.conf
├── .env.example
├── package.json
├── tailwind.config.js
└── vite.config.ts
```

---

## Stack Tecnológica

### Backend
- **Linguagem:** Kotlin 1.9+
- **Framework:** Spring Boot 3.x
- **Banco de dados:** PostgreSQL 15+
- **Migrations:** Flyway
- **Autenticação:** JWT (Spring Security)
- **Documentação:** Springdoc OpenAPI (Swagger UI)
- **Build:** Gradle (Kotlin DSL)

### Frontend
- **Framework:** React 18
- **Linguagem:** TypeScript
- **Build:** Vite
- **Estilização:** Tailwind CSS
- **Componentes:** shadcn/ui
- **Roteamento:** React Router v6
- **Estado global:** Zustand
- **HTTP:** Axios
- **Formulários:** React Hook Form + Zod
- **Gráficos:** Recharts
- **Ícones:** Lucide React
- **Toasts:** react-hot-toast

### Infraestrutura
- **Ambiente dev:** Docker Compose
- **Deploy:** Railway (plano inicial)
- **CI/CD:** Deploy automático via git push na branch `main`

---

## Contribuindo

1. Crie uma branch a partir de `develop`: `git checkout -b feature/nome-da-feature`
2. Faça suas alterações e commite: `git commit -m "feat: descrição"`
3. Abra um Pull Request para `develop`
4. Após validação, `develop` é mergeado em `main` para deploy em produção

> ⚠️ Nunca faça push direto na branch `main` — isso dispara deploy automático em produção.
